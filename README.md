# Clooney Agent (Java Backend Replication)

This is a **working skeleton** for the Clooney backend replication agent, written fully in Java.

It includes:

- Maven project structure
- Main entrypoint
- Orchestrator
- Config loader
- Skeleton agents:
  - APIInspector
  - SpecSynthesizer
  - BackendSynthesizer
  - TestSynthesizer
- LLMClient and Prompts scaffolding

## How to build

```bash
mvn clean package
```

This will produce a runnable JAR:

```bash
java -jar target/clooney-agent-0.0.1-SNAPSHOT.jar
```

Currently the agents are **stubbed** (no real LLM or Playwright calls) so everything compiles and runs without external services. You can fill in the implementations using the design we discussed.
