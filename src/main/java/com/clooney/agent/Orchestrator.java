package com.clooney.agent;

import com.clooney.agent.backend.BackendSynthesizer;
import com.clooney.agent.config.Config;
import com.clooney.agent.inspect.APIInspector;
import com.clooney.agent.llm.LLMClient;
import com.clooney.agent.llm.OpenAiLLMClient;
import com.clooney.agent.llm.StubLLMClient;
import com.clooney.agent.spec.SpecSynthesizer;
import com.clooney.agent.tests.TestSynthesizer;

import java.nio.file.Path;
import java.util.List;

public class Orchestrator {

    private final Config config;

    public Orchestrator(Config config) {
        this.config = config;
    }

    public void runBackendPipeline(List<String> pages, boolean capture) {
        Path logsDir = config.getLogsDir();
        Path specDir = config.getSpecDir();
        Path backendDir = config.getSpringBootOutputDir();
        Path testsDir = config.getTestsOutputDir();

        boolean useStub = Boolean.parseBoolean(
                System.getenv().getOrDefault("CLOONEY_USE_STUB_LLM", "true")
        );

        LLMClient llm = useStub
                ? new StubLLMClient(config.getOpenAiApiKey(), config.getModelName())
                : new OpenAiLLMClient(config.getOpenAiApiKey(), config.getModelName());

        if (capture) {
            APIInspector inspector = new APIInspector(config);
            for (String page : pages) {
                System.out.println("[Clooney] Capturing API calls for " + page);
                inspector.capturePageCalls(page);
            }
        }

        System.out.println("[Clooney] Synthesizing spec...");
        new SpecSynthesizer(logsDir, specDir, llm).synthesize();

        System.out.println("[Clooney] Generating Spring Boot backend...");
        new BackendSynthesizer(specDir, backendDir, llm).synthesizeApp();

        System.out.println("[Clooney] Generating JUnit tests...");
        new TestSynthesizer(specDir, testsDir, llm).synthesizeTests();

        System.out.println("[Clooney] Backend pipeline completed.");
    }
}
