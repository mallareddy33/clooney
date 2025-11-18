package com.clooney.agent;

import com.clooney.agent.backend.BackendSynthesizer;
import com.clooney.agent.config.Config;
import com.clooney.agent.llm.LLMClient;

import java.nio.file.Files;
import java.nio.file.Path;

public class BackendSynthesizerTest {

    public static void main(String[] args) {
        try {
            Config config = Config.loadFromEnv();
            Path specDir = config.getSpecDir();
            Path backendDir = config.getSpringBootOutputDir();

            // 1) Prepare minimal spec files if they don't exist
            Files.createDirectories(specDir);
            Path openapiPath = specDir.resolve("openapi.yaml");
            Path schemaPath = specDir.resolve("schema.sql");

            if (!Files.exists(openapiPath)) {
                String stubOpenApi = """
                    openapi: 3.0.0
                    info:
                      title: Stub API
                      version: 1.0.0
                    paths:
                      /projects:
                        get:
                          responses:
                            '200':
                              description: ok
                    """;
                Files.writeString(openapiPath, stubOpenApi);
            }

            if (!Files.exists(schemaPath)) {
                String stubSchema = """
                    CREATE TABLE projects (
                      id SERIAL PRIMARY KEY,
                      name TEXT NOT NULL
                    );
                    """;
                Files.writeString(schemaPath, stubSchema);
            }

            // 2) Run BackendSynthesizer with LLMClient (which may fall back to stub)
            LLMClient llm = new LLMClient(config.getOpenAiApiKey(), config.getModelName());
            BackendSynthesizer synthesizer = new BackendSynthesizer(specDir, backendDir, llm);

            synthesizer.synthesizeApp();

            System.out.println("Done. Check generated files under: " + backendDir.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("BackendSynthesizerTest failed with exception:");
            e.printStackTrace();
        }
    }
}
