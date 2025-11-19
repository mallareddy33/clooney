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

            writeFilesFromCompletion(completion);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeFilesFromCompletion(String completion) throws IOException {
        Files.createDirectories(outputDir); // e.g. tests/backend/src/test/java/tests/backend

        final String fileMarker = "===FILE:";
        final String endMarker = "===END===";

        int idx = 0;
        while (true) {
            int start = completion.indexOf(fileMarker, idx);
            if (start < 0) {
                break;
            }

            int pathStart = start + fileMarker.length();
            int pathEnd = completion.indexOf("===", pathStart);
            if (pathEnd < 0) {
                break;
            }

            String relativePath = completion.substring(pathStart, pathEnd).trim();

            int contentStart = completion.indexOf('\n', pathEnd);
            if (contentStart < 0) {
                break;
            }
            contentStart += 1; // skip newline

            int nextFile = completion.indexOf(fileMarker, contentStart);
            int end = nextFile >= 0 ? nextFile : completion.indexOf(endMarker, contentStart);
            if (end < 0) {
                end = completion.length();
            }

            String fileContent = completion.substring(contentStart, end).trim();

            Path target = outputDir.resolve(relativePath);
            Files.createDirectories(target.getParent());
            Files.writeString(target, fileContent);

            idx = end;
        }
    }
}
