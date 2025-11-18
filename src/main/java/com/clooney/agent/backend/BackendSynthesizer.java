package com.clooney.agent.backend;

import com.clooney.agent.llm.LLMClient;
import com.clooney.agent.spec.Prompts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BackendSynthesizer {

    private final Path specDir;
    private final Path outputDir;
    private final LLMClient llm;

    public BackendSynthesizer(Path specDir, Path outputDir, LLMClient llm) {
        this.specDir = specDir;
        this.outputDir = outputDir;
        this.llm = llm;
    }

    public void synthesizeApp() {
        try {
            String openapi = Files.readString(specDir.resolve("openapi.yaml"));
            String schemaSql = Files.readString(specDir.resolve("schema.sql"));

            String prompt = Prompts.buildBackendPrompt(openapi, schemaSql);
            String completion = llm.complete(prompt);

            // In stub mode we just ensure outputDir exists and write a placeholder file
            Files.createDirectories(outputDir);
            Path placeholder = outputDir.resolve("README_generated_backend.txt");
            Files.writeString(placeholder, "Backend would be generated here. LLM output was:\n" + completion);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
