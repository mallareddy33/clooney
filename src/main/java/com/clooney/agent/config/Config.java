package com.clooney.agent.config;

import java.nio.file.Path;

public class Config {

    private final String openAiApiKey;
    private final String modelName;
    private final Path projectRoot;

    public Config(String openAiApiKey, String modelName, Path projectRoot) {
        this.openAiApiKey = openAiApiKey;
        this.modelName = modelName;
        this.projectRoot = projectRoot;
    }

    public static Config loadFromEnv() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null) {
            apiKey = "DUMMY_KEY"; // stub value for now
        }
        String model = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4.1-mini");
        Path root = Path.of("").toAbsolutePath();
        return new Config(apiKey, model, root);
    }

    public String getOpenAiApiKey() {
        return openAiApiKey;
    }

    public String getModelName() {
        return modelName;
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
        return projectRoot.resolve("tests/backend");
    }
}
