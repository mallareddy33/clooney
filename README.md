# Clooney – Web App Backend Cloning Agent (Asana-style)

Clooney is an **agentic backend replication system**.

Instead of hand-coding an Asana clone, the agent:

  1. **Observes** the real app’s backend behaviour (HTTP traffic)
  2. **Infers** an API contract + data model (OpenAPI + SQL)
  3. **Generates** a runnable **Spring Boot 3 / Java 17** backend + HTTP tests
  
  This repository implements **Part B (Backend Replication Agent)** of the assignment.

---

## 0. Tech Stack
- Language: Java 17
- Build: Maven
- Generated backend: Spring Boot 3, JPA, H2
- Browser automation: Playwright for Java
- LLM: Pluggable client
    - `StubLLMClient` (default, deterministic, offline)
    - `OpenAiLLMClient` (optional, real model)
- Tests: JUnit 5 + RestAssured

---

## 1. High-Level Architecture

The backend pipeline has **four stages**, orchestrated by a single entrypoint:
  -
    Capture (optional, Playwright)  ->  Spec Synthesis (LLM)  ->  Backend Synthesis (LLM)  ->  Test Synthesis (LLM)
    |                                   |                          |                             |
    raw_*.json logs                    openapi.yaml               Spring Boot project           JUnit + RestAssured
  
    1. **Capture** – Record real HTTP calls from Asana (Home, Projects, Tasks pages).
    2. **Spec Synthesis** – Convert those raw calls into a **clean OpenAPI spec + SQL schema**.
    3. **Backend Synthesis** – Use the spec to generate a **Spring Boot + JPA** backend.
    4. **Test Synthesis** – Generate **JUnit + RestAssured** tests that exercise the generated API.
    
    All stages are orchestrated by a single `Orchestrator` class.

  ###Components
  - Main:
    - CLI entrypoint. Parses flags:
        --mode=backend (currently supported mode)
        --capture (optional – turn on Playwright capture)
        --pages=home,projects,tasks (logical Asana pages)

  - Orchestrator:
    - Wires everything together:
        - APIInspector capture per page.
        - SpecSynthesizer → openapi.yaml + schema.sql.
        - BackendSynthesizer → Spring Boot project.
        - TestSynthesizer → JUnit + RestAssured tests.

  - Config:
    - Centralized configuration. Reads environment variables and resolves all IO paths (logs dir, spec dir, output dirs, etc.).
        - `ASANA_EMAIL`, `ASANA_PASSWORD` (optional login flow)
        - `ASANA_COOKIE` (optional cookie-based auth)

  - LLM layer (LLMClient interface):
    - Pluggable LLM abstraction:
        - StubLLMClient – current default:
            Never calls network.
            Returns deterministic OpenAPI, schema, backend code, and tests.
        - OpenAiLLMClient – real HTTP client for OpenAI:
            Uses OPENAI_API_KEY and OPENAI_MODEL.
            Can be enabled via CLOONEY_USE_STUB_LLM=false.

  - APIInspector (Capture layer):
    - Uses Playwright + Chromium to:
        - Optionally log into Asana (cookie or credentials).
        - Navigate to /0/home, /0/projects, /0/my_tasks.
        - Record HTTP responses targeting Asana’s API.
        - Save them as raw_<page>.json under backend/asana_logs/.

  - SpecSynthesizer (Spec inference):
    - Reads raw_*.json, groups APICall objects by (method, path), builds a JSON summary and asks the LLM (via LLMClient) to generate:
        openapi.yaml (OpenAPI 3.0 spec)
        schema.sql (relational schema)
    - using a strict format with markers:
        ===OPENAPI===
        <OpenAPI 3 YAML>
        ===SCHEMA_SQL===
        <SQL>
        ===END===

  - BackendSynthesizer (Spring Boot codegen):
    - Reads openapi.yaml + schema.sql, builds a backend prompt, calls LLM, and expects a multi-file output with markers:
        ===FILE:pom.xml===
        <pom.xml content>
        ===FILE:src/main/java/.../Application.java===
        <code>
        ...
        ===END===
    - It parses these blocks and writes them under:
        backend/generated/java-backend/

  - TestSynthesizer (Test generation):
    - Reads openapi.yaml, asks the LLM to generate JUnit 5 + RestAssured tests, also using ===FILE:...=== markers, and writes under:
        tests/backend/src/test/java/tests/backend/

## 2. Project Layout
From repo root:
  clooney/
  pom.xml                        # Agent build (Java 17)
  .env.template                  # Example env vars
  
  src/main/java/com/clooney/agent/
  Main.java                    # CLI entrypoint
  Orchestrator.java            # Orchestrates full backend pipeline
  
  config/Config.java           # Loads env, exposes paths
  llm/
  LLMClient.java             # Interface
  StubLLMClient.java         # Default: deterministic, no network
  OpenAiLLMClient.java       # Optional: real OpenAI calls
  
  inspect/
  APIInspector.java          # Playwright-based HTTP capture
  APIInspectorTest.java      # Simple main() helper
  
  spec/
  SpecSynthesizer.java       # Logs -> OpenAPI + schema.sql
  Prompts.java               # All LLM prompts (spec/backend/tests)
  
  backend/
  BackendSynthesizer.java    # OpenAPI+schema -> Spring Boot backend
  BackendSynthesizerTest.java# Simple main() helper
  
  tests/
  TestSynthesizer.java       # OpenAPI -> JUnit + RestAssured tests
  
  backend/
  asana_logs/
  raw_home.json              # Raw captured (or synthetic) logs
  raw_projects.json
  raw_tasks.json
  raw_events.json            # Optional extra
  
  generated/
  openapi.yaml               # Synthesized API spec
  schema.sql                 # Synthesized SQL schema
  java-backend/              # Generated Spring Boot app (pom + src)
  
  tests/
  backend/
  pom.xml                    # Maven project for generated tests
  src/test/java/tests/backend/
  ProjectsApiTests.java    # Generated or stubbed tests
  TasksApiTests.java

## 3. Configuration (.env and .env.template)
Configuration is centralized in Config :
  - 3.1 Required:
      OPENAI_API_KEY=your-openai-key-or-placeholder
      OPENAI_MODEL=gpt-4.1-mini   # or any compatible model
      
      Even if you use stub mode, OPENAI_API_KEY is required by Config.loadFromEnv()
      but the stub client will not call the network.
  - 3.2 Optional - Asana login (for live capture):
      You can use either cookie auth or email/password.
      - Option 1: Cookie-based auth (recommended if used)
      ASANA_COOKIE='name=value; name2=value2; ...'

      - Option 2: Email/password-based login (best-effort)
      ASANA_EMAIL=you@example.com
      ASANA_PASSWORD=strong-password
      
      Live capture is optional and may be flaky due to SSO/MFA/CAPTCHA etc.
      This repo is designed so that the pipeline still works purely off
      pre-recorded logs in backend/asana_logs/.

  - 3.3 LLM Mode:
      # Default: deterministic stub client (no network)
      CLOONEY_USE_STUB_LLM=true

      # Optional: use real OpenAI client
      CLOONEY_USE_STUB_LLM=false

  - 3.4 Loading .env (bash/zsh):
      - 1. Copy template :
          ```bash
          cp .env.template .env # edit .env and fill in values
      - 2. Load into current shell (simple case):
          ```bash
          export $(grep -v '^#' .env | xargs)

## 4. LLM Modes (Stub vs Real)
The agent uses a pluggable LLMClient interface:
  - StubLLMClient (default) :
      No external network calls.
      - Returns deterministic:
          openapi.yaml
          schema.sql
          Spring Boot project (with /projects and /tasks)
          JUnit + RestAssured tests.
      - Ideal for deterministic local development and evaluation.
  - OpenAiLLMClient (optional) :
      Uses Java’s HttpClient to call OpenAI’s Chat Completions API.
      Reads OPENAI_API_KEY and OPENAI_MODEL.
      Same prompts, same parsing, only the underlying model changes.
Orchestrator chooses implementation based on:
  CLOONEY_USE_STUB_LLM=true   # => StubLLMClient (default)
  CLOONEY_USE_STUB_LLM=false  # => OpenAiLLMClient
  - Example for real mode (optional):
    ```bash
    export CLOONEY_USE_STUB_LLM=false
    export OPENAI_API_KEY=sk-...
    export OPENAI_MODEL=gpt-4.1-mini

## 5. How to Build and Run (Step-by-Step)
Below is a complete “press these buttons in this order” guide. :
  - 5.1 Build the agent:
      From repo root:
        ```bash
        mvn clean package
       This produces target/clooney-agent-0.0.1-SNAPSHOT.jar

  - 5.2 Run the pipeline without live capture :
      This uses pre-recorded logs and the LLM client (stub by default).
      - From repo root:
      # Ensure env vars are loaded
        ```bash
        export $(grep -v '^#' .env | xargs)
      
      # Run backend pipeline (no --capture)
        ```bash
        mvn -q -DskipTests exec:java \
        -Dexec.mainClass=com.clooney.agent.Main \
        -Dexec.args="--mode=backend --pages=home,projects,tasks"

    What this does:
      - Capture step: skipped (no --capture flag).
      - SpecSynthesizer: reads logs under backend/asana_logs/ → writes backend/generated/openapi.yaml + schema.sql.
      - BackendSynthesizer: reads spec → generates Spring Boot project under backend/generated/java-backend/.
      - TestSynthesizer: reads OpenAPI → generates tests under tests/backend/src/test/java/tests/backend/.

  - 5.3 Run the generated backend :
      - After the pipeline finishes:
          ```bash
          cd backend/generated/java-backend
          mvn spring-boot:run
        
      - Spring Boot runs on port 8080.
      - Check endpoints in another terminal:
          ```bash
          curl http://localhost:8080/projects
          curl http://localhost:8080/tasks
      - You should see JSON responses like:
          {"data":[{"gid":"...","name":"Website Revamp"}, ...]}

  - 5.4 Run the generated tests :
      In another terminal:
        ```bash
        cd tests/backend
        mvn test

      What this uses:
        - tests/backend/pom.xml – JUnit 5 + RestAssured.
        - ProjectsApiTests.java & TasksApiTests.java – generated or stubbed tests under tests/backend/src/test/java/tests/backend/.

      The tests assume:
        - Backend is running at http://localhost:8080.
         - It exposes /projects and /tasks returning a {"data": [...]} structure.

  - 5.5 Run with live capture :
      If you want to exercise the full agentic pipeline including Playwright capture:
        - 1. Ensure either:
            ```bash
            ASANA_COOKIE='name=value; name2=value2; ...'
            # or
            ```bash
            ASANA_EMAIL=you@example.com
            ASANA_PASSWORD=your-password
        - 2. Run:
            ```bash
            mvn -q -DskipTests exec:java \
            -Dexec.mainClass=com.clooney.agent.Main \
            -Dexec.args="--mode=backend --capture --pages=home,projects,tasks"

      What changes:
        APIInspector launches Chromium, authenticates, navigates to the pages, and records HTTP calls into
          ```bash
          backend/asana_logs/raw_home.json
          backend/asana_logs/raw_projects.json
          backend/asana_logs/raw_tasks.json
        
        SpecSynthesizer then builds a spec & schema from these fresh logs.
        Backend & tests are generated from the new spec.
        Real-world note - 
          Asana auth/SSO/MFA, org configuration, and internal API URLs may vary.
          For deterministic evaluation, the non-capture path using pre-populated logs
          is recommended and fully supported.

## 6. What the Evaluator Can Do (TL;DR)
  - From a fresh clone:
      ```bash
      git clone <repo>
  - From a zip:
      unzip clooney-agent.zip
      
      ```bash
      cd clooney

      cp .env.template .env
      # edit .env and set OPENAI_API_KEY + OPENAI_MODEL (stub mode still requires them)

      export $(grep -v '^#' .env | xargs)

      mvn clean package

      mvn -q -DskipTests exec:java \
      -Dexec.mainClass=com.clooney.agent.Main \
      -Dexec.args="--mode=backend --pages=home,projects,tasks"

  - Then:
      ```bash    
      cd backend/generated/java-backend
      mvn spring-boot:run

  - In another terminal:
      ```bash
      curl http://localhost:8080/projects
      curl http://localhost:8080/tasks

  - (Optional tests):
      ```bash
      cd tests/backend
      mvn test

# 7. Notes :
  - Live Asana capture via Playwright is best-effort, only SSO/MFA can break it. → Pre-recorded logs fully supported.
  - Stub LLM mode is the default for deterministic behaviour.
  - Real LLM mode (OpenAiLLMClient) uses a simple Chat Completions-style API call and can be enabled via CLOONEY_USE_STUB_LLM=false.
