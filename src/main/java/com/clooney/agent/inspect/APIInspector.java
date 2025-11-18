package com.clooney.agent.inspect;

import com.clooney.agent.config.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Real API inspector using Playwright.
 *
 * Responsibilities:
 *  - Launch a headless Chromium browser.
 *  - Authenticate into Asana (email/password or cookie) if possible.
 *  - Navigate to a target page (home/projects/tasks).
 *  - Capture all HTTP responses whose URLs contain "/api/1.0/".
 *  - For each captured call, store:
 *      method, url, path (normalized), query params, request body, status, response body.
 *  - Persist captured calls into backend/asana_logs/raw_<page>.json
 */
public class APIInspector {

    private final Config config;
    private final Path outputDir;
    private final ObjectMapper mapper = new ObjectMapper();

    public APIInspector(Config config) {
        this.config = config;
        this.outputDir = config.getLogsDir();
    }

    /**
     * Capture API calls for a logical Asana page: "home", "projects", "tasks".
     * Writes output as JSON to backend/asana_logs/raw_<pageName>.json
     *
     * @param pageName one of: home, projects, tasks
     * @return list of captured APICall objects
     */
    public List<APICall> capturePageCalls(String pageName) {
        String url = switch (pageName) {
            case "home" -> "https://app.asana.com/0/home";
            case "projects" -> "https://app.asana.com/0/projects";
            case "tasks", "my_tasks" -> "https://app.asana.com/0/my_tasks";
            default -> throw new IllegalArgumentException("Unknown page: " + pageName);
        };

        List<APICall> captured = new ArrayList<>();

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );

            BrowserContext context = createContextWithOptionalCookies(browser);
            Page page = context.newPage();

            // Attach response listener BEFORE navigation
            context.onResponse(response -> {
                try {
                    String respUrl = response.url();
                    if (!respUrl.contains("/api/1.0/")) {
                        return;
                    }

                    APICall call = buildApiCallFromResponse(response);
                    synchronized (captured) {
                        captured.add(call);
                    }
                } catch (Exception e) {
                    // swallow individual errors to keep capture robust
                    System.err.println("[Clooney] Error capturing response: " + e.getMessage());
                }
            });

            // Login if we have credentials and no cookie
            if (shouldLoginWithCredentials()) {
                System.out.println("[Clooney] Logging into Asana using email/password...");
                loginToAsana(page, config.getAsanaEmail(), config.getAsanaPassword());
            } else if (config.getAsanaCookie() != null && !config.getAsanaCookie().isBlank()) {
                System.out.println("[Clooney] Using ASANA_COOKIE for authentication (no login flow).");
            } else {
                System.out.println("[Clooney] WARNING: No Asana credentials or cookie provided. " +
                        "You might be redirected to login instead of dashboard.");
            }

            System.out.println("[Clooney] Navigating to page: " + url);
            page.navigate(url);

            // Let the page load and API calls fire; tweak timeout as needed
            page.waitForTimeout(8000);

            browser.close();
        }

        // Persist captured calls
        try {
            Files.createDirectories(outputDir);
            Path file = outputDir.resolve("raw_" + pageName + ".json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), captured);
            System.out.println("[Clooney] Captured " + captured.size() +
                    " calls for page '" + pageName + "' into " + file.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write captured calls", e);
        }

        return captured;
    }

    // ----------------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------------

    private BrowserContext createContextWithOptionalCookies(Browser browser) {
        BrowserContext context = browser.newContext();

        // Optional cookie-based auth
        String cookieHeader = config.getAsanaCookie();
        if (cookieHeader != null && !cookieHeader.isBlank()) {
            // Expecting something like: "name=value; name2=value2"
            List<BrowserContext.AddCookiesParam> cookies = parseCookieHeader(cookieHeader);
            if (!cookies.isEmpty()) {
                context.addCookies(cookies.toArray(new BrowserContext.AddCookiesParam[0]));
            }
        }

        return context;
    }

    private boolean shouldLoginWithCredentials() {
        return config.getAsanaEmail() != null && !config.getAsanaEmail().isBlank()
                && config.getAsanaPassword() != null && !config.getAsanaPassword().isBlank();
    }

    private void loginToAsana(Page page, String email, String password) {
        System.out.println("[Clooney] Starting Asana login flow...");

        // Initial login page
        page.navigate("https://app.asana.com/-/login");
        page.waitForTimeout(2000);

        // ⚠️ These selectors may change if Asana updates their login UI.
        // Inspect once with dev tools and tweak if needed.
        try {
            page.fill("input[type='email']", email);
            page.click("button[type='submit']");
        } catch (PlaywrightException e) {
            System.err.println("[Clooney] Email step selectors failed: " + e.getMessage());
        }

        page.waitForTimeout(3000);

        try {
            page.fill("input[type='password']", password);
            page.click("button[type='submit']");
        } catch (PlaywrightException e) {
            System.err.println("[Clooney] Password step selectors failed: " + e.getMessage());
        }

        // Give time for redirects and dashboard load
        page.waitForTimeout(5000);
        System.out.println("[Clooney] Login attempt completed (check manually during dev if needed).");
    }

    private APICall buildApiCallFromResponse(Response response) {
        String respUrl = response.url();
        Request req = response.request();

        APICall call = new APICall();
        call.method = req.method();
        call.url = respUrl;

        ParsedUrl parsed = parseUrl(respUrl);
        call.path = parsed.path();
        call.query = parsed.query();

        call.status = response.status();

        // Request body
        String postData = req.postData();
        if (postData != null) {
            call.requestBody = tryParseJson(postData);
        }

        // Response body
        String bodyText = safeReadBody(response);
        call.responseBody = tryParseJson(bodyText);

        return call;
    }

    private String safeReadBody(Response response) {
        try {
            return response.text();
        } catch (PlaywrightException e) {
            return "";
        }
    }

    private Object tryParseJson(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return mapper.readValue(text, Object.class);
        } catch (Exception e) {
            // If it's not JSON, just store raw text
            return text;
        }
    }

    private ParsedUrl parseUrl(String url) {
        try {
            URI uri = new URI(url);
            String rawPath = uri.getPath();
            // Normalize by stripping the /api/1.0 prefix
            String path = rawPath.replaceFirst("/api/1.0", "");

            Map<String, Object> queryMap = new LinkedHashMap<>();
            String query = uri.getQuery();
            if (query != null && !query.isBlank()) {
                for (String kvPair : query.split("&")) {
                    String[] kv = kvPair.split("=", 2);
                    String key = kv[0];
                    String value = kv.length > 1 ? kv[1] : "";
                    queryMap.put(key, value);
                }
            }
            return new ParsedUrl(path, queryMap);
        } catch (URISyntaxException e) {
            throw new RuntimeException("Failed to parse URL: " + url, e);
        }
    }

    private List<BrowserContext.AddCookiesParam> parseCookieHeader(String cookieHeader) {
        List<BrowserContext.AddCookiesParam> cookies = new ArrayList<>();
        String[] parts = cookieHeader.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;

            String[] kv = trimmed.split("=", 2);
            if (kv.length != 2) continue;
            String name = kv[0].trim();
            String value = kv[1].trim();

            BrowserContext.AddCookiesParam cookie = new BrowserContext.AddCookiesParam();
            cookie.setName(name);
            cookie.setValue(value);
            cookie.setDomain("app.asana.com");
            cookie.setPath("/");
            cookies.add(cookie);
        }
        return cookies;
    }

    // Simple struct for internal URL parse result
    private record ParsedUrl(String path, Map<String, Object> query) {}

    /**
     * DTO representing a single captured API call.
     * Jackson-friendly with public fields.
     */
    public static class APICall {
        public String method;
        public String url;
        public String path;
        public Map<String, Object> query;
        public Object requestBody;
        public int status;
        public Object responseBody;

        public APICall() {
        }
    }
}
