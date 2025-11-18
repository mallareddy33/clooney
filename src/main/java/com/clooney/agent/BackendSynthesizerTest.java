package com.clooney.agent;

import com.clooney.agent.backend.BackendSynthesizer;
import com.clooney.agent.config.Config;
import com.clooney.agent.llm.LLMClient;

import java.nio.file.Files;
import java.nio.file.Path;

public class BackendSynthesizerTest {

    public static void main(String[] args) {
        try {
            System.out.println("[Test] Starting BackendSynthesizerTest...");

            Config config = Config.loadFromEnv();
            System.out.println("[Test] Loaded config. projectRoot=" + config.getProjectRoot());

            Path specDir = config.getSpecDir();
            Path backendDir = config.getSpringBootOutputDir();
            System.out.println("[Test] specDir=" + specDir.toAbsolutePath());
            System.out.println("[Test] backendDir=" + backendDir.toAbsolutePath());

            // 1) Prepare minimal spec files if they don't exist
            Files.createDirectories(specDir);
            Path openapiPath = specDir.resolve("openapi.yaml");
            Path schemaPath = specDir.resolve("schema.sql");

            if (!Files.exists(openapiPath)) {
                System.out.println("[Test] Writing stub openapi.yaml...");
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
                System.out.println("[Test] Writing stub schema.sql...");
                String stubSchema = """
                    CREATE TABLE projects (
                      id SERIAL PRIMARY KEY,
                      name TEXT NOT NULL
                    );
                    """;
                Files.writeString(schemaPath, stubSchema);
            }

            System.out.println("[Test] Creating LLMClient...");
            LLMClient llm = new LLMClient(config.getOpenAiApiKey(), config.getModelName());

            System.out.println("[Test] Running BackendSynthesizer...");
            BackendSynthesizer synthesizer = new BackendSynthesizer(specDir, backendDir, llm);
            synthesizer.synthesizeApp();

            System.out.println("[Test] Done. Check generated files under: " + backendDir.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("BackendSynthesizerTest failed with exception:");
            e.printStackTrace();
        }
    }
}
