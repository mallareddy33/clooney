package com.clooney.agent;

import com.clooney.agent.config.Config;

import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        String mode = "backend";
        String pagesArg = "home,projects,tasks";
        boolean capture = false;

        for (String arg : args) {
            if (arg.startsWith("--mode=")) {
                mode = arg.substring("--mode=".length());
            } else if (arg.startsWith("--pages=")) {
                pagesArg = arg.substring("--pages=".length());
            } else if (arg.equals("--capture")) {
                capture = true;
            }
        }

        List<String> pages = Arrays.stream(pagesArg.split(","))
                .map(String::trim)
                .toList();

        Config config = Config.loadFromEnv();
        Orchestrator orchestrator = new Orchestrator(config);

        if (mode.contains("backend")) {
            orchestrator.runBackendPipeline(pages, capture);
        } else {
            System.err.println("Unsupported mode: " + mode);
        }
    }
}
