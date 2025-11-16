package com.clooney.agent.tests;

import com.clooney.agent.llm.LLMClient;
import com.clooney.agent.spec.Prompts;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestSynthesizer {

    private final Path specDir;
    private final Path outputDir;
    private final LLMClient llm;

    public TestSynthesizer(Path specDir, Path outputDir, LLMClient llm) {
        this.specDir = specDir;
        this.outputDir = outputDir;
        this.llm = llm;
    }

    public void synthesizeTests() {
        try {
            String openapi = Files.readString(specDir.resolve("openapi.yaml"));
            String prompt = Prompts.buildTestsPrompt(openapi);
            String completion = llm.complete(prompt);

            Files.createDirectories(outputDir);
            Path placeholder = outputDir.resolve("README_generated_tests.txt");
            Files.writeString(placeholder, "Tests would be generated here. LLM output was:\n" + completion);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
