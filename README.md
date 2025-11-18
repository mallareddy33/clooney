## Architecture

Clooney is an **agentic backend replication system**: instead of hand-coding an Asana clone, we build an **agent** that can:
  
  1. Observe the real app’s backend behaviour (HTTP traffic).
  2. Infer an API contract + data model from these traces.
  3. Generate a runnable Spring Boot backend + tests from that inferred spec.
  
  This repository contains the **backend replication agent only** (Part B of the assignment).

---

### High-Level Design

At a high level the system has four main stages:
  
  1. **Capture** – Record real HTTP calls from Asana (Home, Projects, Tasks pages).
  2. **Spec Synthesis** – Convert those raw calls into a **clean OpenAPI spec + SQL schema**.
  3. **Backend Synthesis** – Use the spec to generate a **Spring Boot + JPA** backend.
  4. **Test Synthesis** – Generate **JUnit + RestAssured** tests that exercise the generated API.
  
  All stages are orchestrated by a single `Orchestrator` class, and the stages use an LLM (OpenAI by default) via a thin `LLMClient` wrapper.

---

### Main Components

#### 1. `Config`

Centralised configuration loaded from environment (or `.env`):

  - OpenAI:
      - `OPENAI_API_KEY`
      - `OPENAI_MODEL` (e.g. `gpt-4.1-mini`)
  - Asana:
      - `ASANA_EMAIL`, `ASANA_PASSWORD` (optional login flow)
      - `ASANA_COOKIE` (optional cookie-based auth)
  - Paths:
      - `logsDir` → `backend/asana_logs`
      - `specDir` → `backend/generated/spec`
      - `springBootOutputDir` → `backend/generated/java-backend`
      - `testsOutputDir` → `tests/backend`
  
  `Config` gives everyone a single source of truth for IO paths and secrets.

---

#### 2. `APIInspector` (Capture layer)

- Technology: **Playwright for Java** (Chromium headless).
- Responsibility:
    - Open Asana in a real browser session.
    - Authenticate either via:
        - `ASANA_COOKIE` (preferred for CI/evaluation), or
        - `ASANA_EMAIL` + `ASANA_PASSWORD` (login flow).
    - Navigate to:
        - `/0/home`
        - `/0/projects`
        - `/0/my_tasks`
    - Listen to **all network responses** and record only those hitting Asana’s REST API (`/api/1.0/...`).
- For each captured call, it records:
    - HTTP method, full URL, normalized path
    - Query params
    - Request body (parsed as JSON when possible)
    - Response status and body
- Output:
    - One JSON file per page:
        - `backend/asana_logs/raw_home.json`
        - `backend/asana_logs/raw_projects.json`
        - `backend/asana_logs/raw_tasks.json`
  
  This gives us **real, high-fidelity API behaviour** instead of guessing.

---

#### 3. `SpecSynthesizer` (API + Schema inference)

- Input: the raw log files from `APIInspector`.
- Responsibility:
    - Group similar calls by `(method, normalized path)`.
    - Summarize the example requests/responses into a compact JSON summary.
    - Call the LLM via `LLMClient` using `Prompts.buildSpecPrompt(summaryJson)`.
- The LLM is instructed to:
    - Emit an **OpenAPI 3.0 YAML** spec for the observed endpoints.
    - Emit a **relational SQL schema** (tables, columns, basic constraints).
    - Wrap outputs in clear markers:
        - `===OPENAPI=== ... ===SCHEMA_SQL=== ... ===END===`
- Output:
    - `backend/generated/spec/openapi.yaml`
    - `backend/generated/spec/schema.sql`
  
  This stage turns noisy, real-world HTTP traffic into a **clean contract**.

---

#### 4. `BackendSynthesizer` (Spring Boot codegen)

- Input:
    - `openapi.yaml`
    - `schema.sql`
- Responsibility:
    - Build a backend-generation prompt via `Prompts.buildBackendPrompt(openapiYaml, schemaSql)`.
    - Call `LLMClient` to generate **multi-file code**.
    - Parse code blocks in a simple, deterministic format:
      ```text
      ===FILE:backend/generated/java-backend/pom.xml===
      <pom.xml>
      ===FILE:backend/generated/java-backend/src/main/java/com/clooney/generated/Application.java===
      <Application.java>
      ...
      ===END===
      ```
    - Write each file to disk under `backend/generated/java-backend`.
- Generated project:
    - `pom.xml` with:
         - `spring-boot-starter-web`
         - `spring-boot-starter-data-jpa`
         - `com.h2database:h2` (in-memory DB so the app runs out-of-the-box)
    - `src/main/java/com/clooney/generated/Application.java`
    - Controller / service / repository / entity packages (based on OpenAPI + schema).
    - `src/main/resources/application.yml` with:
         - H2 datasource config
         - Basic JPA settings

The goal is that the evaluator can:
  
  ```bash
  cd backend/generated/java-backend
  mvn spring-boot:run
