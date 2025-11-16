package com.clooney.agent.llm;

public class LLMClient {

    private final String apiKey;
    private final String model;

    public LLMClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * Stub implementation.
     * In the real agent, you would call OpenAI here.
     */
    public String complete(String prompt) {
        // Return a minimal dummy response with expected markers
        return "===OPENAPI===\nopenapi: 3.0.0\ninfo:\n  title: Dummy\n  version: 1.0.0\npaths: {}\n" +
               "===SCHEMA_SQL===\n-- dummy schema\n" +
               "===END===";
    }
}
