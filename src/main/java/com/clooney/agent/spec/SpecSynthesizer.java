package com.clooney.agent.spec;

import com.clooney.agent.inspect.APICall;
import com.clooney.agent.llm.LLMClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpecSynthesizer {

    private final Path logsDir;
    private final Path outputDir;
    private final LLMClient llm;
    private final ObjectMapper mapper = new ObjectMapper();

    public SpecSynthesizer(Path logsDir, Path outputDir, LLMClient llm) {
        this.logsDir = logsDir;
        this.outputDir = outputDir;
        this.llm = llm;
    }

    public void synthesize() {
        List<APICall> allCalls = loadAllCalls();
        Map<String, List<APICall>> clusters = clusterByEndpoint(allCalls);
        String prompt = buildPrompt(clusters);
        String completion = llm.complete(prompt);

        String openapi = extractBetween(completion, "===OPENAPI===", "===SCHEMA_SQL===");
        String schemaSql = extractBetween(completion, "===SCHEMA_SQL===", "===END===");

        try {
            Files.createDirectories(outputDir);
            Files.writeString(outputDir.resolve("openapi.yaml"), openapi);
            Files.writeString(outputDir.resolve("schema.sql"), schemaSql);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<APICall> loadAllCalls() {
        List<APICall> res = new ArrayList<>();
        if (!Files.exists(logsDir)) return res;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logsDir, "raw_*.json")) {
            for (Path file : stream) {
                APICall[] arr = mapper.readValue(file.toFile(), APICall[].class);
                res.addAll(Arrays.asList(arr));
            }
        } catch (IOException e) {
            // ignore in stub
        }
        return res;
    }

    private Map<String, List<APICall>> clusterByEndpoint(List<APICall> calls) {
        return calls.stream().collect(Collectors.groupingBy(
                c -> c.method + " " + c.path
        ));
    }

    private String buildPrompt(Map<String, List<APICall>> clusters) {
        try {
            String summaryJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(clusters);
            return Prompts.buildSpecPrompt(summaryJson);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String extractBetween(String text, String start, String end) {
        int s = text.indexOf(start);
        if (s < 0) return "";
        s += start.length();
        int e = text.indexOf(end, s);
        if (e < 0) e = text.length();
        return text.substring(s, e).trim();
    }
}
