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
            Path openapiPath = specDir.resolve("openapi.yaml");
            Path schemaPath = specDir.resolve("schema.sql");

            if (!Files.exists(openapiPath)) {
                throw new IllegalStateException("openapi.yaml not found in " + specDir.toAbsolutePath());
            }
            if (!Files.exists(schemaPath)) {
                throw new IllegalStateException("schema.sql not found in " + specDir.toAbsolutePath());
            }

            String openapi = Files.readString(openapiPath);
            String schemaSql = Files.readString(schemaPath);

            System.out.println("[Clooney] Calling LLM to generate Spring Boot backend...");
            String prompt = Prompts.buildBackendPrompt(openapi, schemaSql);
            String completion = llm.complete(prompt);

            Files.createDirectories(outputDir);
            int fileCount = writeFilesFromCompletion(completion);

            if (fileCount == 0) {
                // No markers found; write raw content for debugging
                Path raw = outputDir.resolve("LLM_BACKEND_RAW.txt");
                Files.writeString(raw, completion);
                System.out.println("[Clooney] No ===FILE:...=== markers found in LLM output. " +
                        "Raw output written to " + raw.toAbsolutePath());
            } else {
                System.out.println("[Clooney] Generated " + fileCount +
                        " backend files under " + outputDir.toAbsolutePath());
            }

        } catch (IOException e) {
            throw new RuntimeException("BackendSynthesizer failed", e);
        }
    }

    private int writeFilesFromCompletion(String completion) throws IOException {
        String marker = "===FILE:";
        int idx = completion.indexOf(marker);
        int written = 0;

        while (idx >= 0) {
            // Find end of the header line: "===FILE:...===\n"
            int endHeader = findEndOfHeader(completion, idx);
            if (endHeader < 0) {
                break; // malformed, stop parsing
            }

            // Extract header path between "===FILE:" and "==="
            String header = completion.substring(idx + marker.length(), endHeader).trim();
            // Next file marker
            int nextFileIdx = completion.indexOf(marker, endHeader);

            // Body is between endHeader and nextFileIdx (or end of string)
            String body;
            if (nextFileIdx >= 0) {
                body = completion.substring(endHeader, nextFileIdx);
            } else {
                // If there's an ===END=== marker at the end, ignore it
                String tail = completion.substring(endHeader);
                int endIdx = tail.indexOf("===END===");
                if (endIdx >= 0) {
                    body = tail.substring(0, endIdx);
                } else {
                    body = tail;
                }
            }
            body = stripLeadingNewline(body);

            Path targetPath = resolveTargetPath(header);
            Files.createDirectories(targetPath.getParent());
            Files.writeString(targetPath, body);

            System.out.println("[Clooney] Generated file: " + targetPath.toAbsolutePath());
            written++;

            idx = nextFileIdx;
        }

        return written;
    }

    private int findEndOfHeader(String text, int fileMarkerIndex) {
        // header starts at "===FILE:"; we want position after the trailing "===<newline>"
        int afterMarker = text.indexOf("===\n", fileMarkerIndex);
        int newlineLen = 4;
        if (afterMarker < 0) {
            afterMarker = text.indexOf("===\r\n", fileMarkerIndex);
            newlineLen = 5;
        }
        if (afterMarker < 0) {
            return -1;
        }
        return afterMarker + newlineLen;
    }

    private String stripLeadingNewline(String body) {
        if (body.startsWith("\r\n")) {
            return body.substring(2);
        }
        if (body.startsWith("\n")) {
            return body.substring(1);
        }
        return body;
    }

    private Path resolveTargetPath(String headerPath) {
        String relative = headerPath;

        String prefix = "backend/generated/java-backend/";
        if (relative.startsWith(prefix)) {
            relative = relative.substring(prefix.length());
        }

        // Guard against path traversal
        Path target = outputDir.resolve(relative).normalize();
        if (!target.startsWith(outputDir.normalize())) {
            throw new IllegalArgumentException("Refusing to write file outside outputDir: " + headerPath);
        }

        return target;
    }
}