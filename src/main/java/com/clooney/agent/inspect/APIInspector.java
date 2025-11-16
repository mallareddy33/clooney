package com.clooney.agent.inspect;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Stub inspector. In the full implementation, this would use Playwright
 * to capture real Asana API calls. For now, it just writes an empty list.
 */
public class APIInspector {

    private final Path outputDir;
    private final ObjectMapper mapper = new ObjectMapper();

    public APIInspector(Path outputDir) {
        this.outputDir = outputDir;
    }

    public List<APICall> capturePageCalls(String pageName) {
        List<APICall> calls = new ArrayList<>();
        try {
            Files.createDirectories(outputDir);
            Path file = outputDir.resolve("raw_" + pageName + ".json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), calls);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return calls;
    }
}
