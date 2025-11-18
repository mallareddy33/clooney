package com.clooney.agent;

import com.clooney.agent.config.Config;
import com.clooney.agent.inspect.APIInspector;

public class APIInspectorTest {
    public static void main(String[] args) {
        try {
            Config config = Config.loadFromEnv();
            APIInspector inspector = new APIInspector(config);
            inspector.capturePageCalls("home");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
