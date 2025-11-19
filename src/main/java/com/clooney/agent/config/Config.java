package com.clooney.agent.config;

import java.nio.file.Path;

public class Config {

    // Required
    private final String openAiApiKey;
    private final String modelName;

    // Optional fields for Asana login
    private final String asanaEmail;
    private final String asanaPassword;
    private final String asanaCookie;

    // Project root for output generation
    private final Path projectRoot;

    public Config(
            String openAiApiKey,
            String modelName,
            Path projectRoot,
            String asanaEmail,
            String asanaPassword,
            String asanaCookie
    ) {
        this.openAiApiKey = openAiApiKey;
        this.modelName = modelName;
        this.projectRoot = projectRoot;
        this.asanaEmail = asanaEmail;
        this.asanaPassword = asanaPassword;
        this.asanaCookie = asanaCookie;
    }

    // ===========================
    // LOAD FROM ENVIRONMENT
    // ===========================
    public static Config loadFromEnv() {

        // ---------------------------
        // Required: LLM API key
        // ---------------------------
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "ERROR: OPENAI_API_KEY missing.\n" +
                            "Please copy .env.template → .env and add your secret keys.\n" +
                            "Then run:\n" +
                            "export $(grep -v '^#' .env | xargs)\n"
            );
        }

        // ---------------------------
        // Optional: Model name
        // ---------------------------
        String model = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4.1-mini");

        // ---------------------------
        // Optional: Asana login fields
        // (Needed for full Playwright capture)
        // ---------------------------
        String email = System.getenv("ASANA_EMAIL");
        String password = System.getenv("ASANA_PASSWORD");
        String cookie = System.getenv("ASANA_COOKIE");

        // ---------------------------
        // Project root path
        // ---------------------------
        Path root = Path.of("").toAbsolutePath();

        return new Config(apiKey, model, root, email, password, cookie);
    }

    // ===========================
    // GETTERS
    // ===========================
    public String getOpenAiApiKey() {
        return openAiApiKey;
    }

    public String getModelName() {
        return modelName;
    }

    public String getAsanaEmail() {
        return asanaEmail;
    }

    public String getAsanaPassword() {
        return asanaPassword;
    }

    public String getAsanaCookie() {
        return asanaCookie;
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    public Path getLogsDir() {
        return projectRoot.resolve("backend/asana_logs");
    }

    public Path getSpecDir() {
        return projectRoot.resolve("backend/generated");
    }

    public Path getSpringBootOutputDir() {
        return projectRoot.resolve("backend/generated/java-backend");
    }

    public Path getTestsOutputDir() {
        return projectRoot.resolve("tests/backend/src/test/java/tests/backend");
    }
}