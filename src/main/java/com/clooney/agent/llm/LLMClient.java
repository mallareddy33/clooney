package com.clooney.agent.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Production-ready LLM client for OpenAI endpoints.
 * Uses HTTP instead of OpenAI SDK to avoid dependency bloat.
 */
public class LLMClient {

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    // Retry parameters
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_BACKOFF_MS = 1200;

    public LLMClient(String apiKey, String model) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is missing. Please set in environment.");
        }
        this.apiKey = apiKey;
        this.model = model;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    /**
     * Main chat completion method.
     * Sends a non-streaming request to OpenAI.
     */
    public String complete(String prompt) {
        JsonNode requestBody = buildPayload(prompt);

        String requestJson;
        try {
            requestJson = mapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize request JSON", e);
        }

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                        .timeout(Duration.ofSeconds(120))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + this.apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString()
                );

                int status = response.statusCode();
                if (status == 200) {
                    return extractContent(response.body());
                }

                // transient failures need retry
                if (status == 429 || status >= 500) {
                    System.err.println("[LLMClient] Transient failure (" + status +
                            "). Retrying in " + RETRY_BACKOFF_MS + " ms...");
                    Thread.sleep(RETRY_BACKOFF_MS);
                    continue;
                }

                // fatal errors
                System.err.println("[LLMClient] Fatal LLM API error: \n" + response.body());
                throw new RuntimeException("LLM API returned status " + status);

            } catch (IOException | InterruptedException ex) {
                System.err.println("[LLMClient] Network error: " + ex.getMessage());
                try {
                    Thread.sleep(RETRY_BACKOFF_MS);
                } catch (InterruptedException ignored) {}
            }
        }

        throw new RuntimeException("LLMClient: Failed after " + MAX_RETRIES + " retry attempts");
    }

    /**
     * Build JSON payload structure for OpenAI chat completion.
     */
    private JsonNode buildPayload(String prompt) {
        return mapper.createObjectNode()
                .put("model", model)
                .put("temperature", 0.1)
                .set("messages", mapper.createArrayNode()
                        .add(
                                mapper.createObjectNode()
                                        .put("role", "user")
                                        .put("content", prompt)
                        )
                );
    }

    /**
     * Extract the assistant message from OpenAI JSON.
     */
    private String extractContent(String response) {
        try {
            JsonNode root = mapper.readTree(response);

            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.size() == 0) {
                throw new RuntimeException("No choices returned by LLM");
            }

            return choices.get(0).get("message").get("content").asText();

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }
}
