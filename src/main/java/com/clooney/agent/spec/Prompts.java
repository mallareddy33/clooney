package com.clooney.agent.spec;

public class Prompts {

    /**
     * Prompt for turning clustered API call observations into:
     *  - an OpenAPI 3.0 spec
     *  - a SQL schema
     *
     * The LLMClient stub looks for the markers:
     *  - ===OPENAPI===
     *  - ===SCHEMA_SQL===
     *  - ===END===
     */
    public static String buildSpecPrompt(String summaryJson) {
        return """
You are an expert backend engineer and API designer.
You will be given a JSON object that groups observed HTTP API calls
by method + path. The JSON contains, for each endpoint, examples of:

- HTTP method
- URL + path
- query parameters
- request bodies
- response bodies
- status codes

From this, infer:

1) A best-effort OpenAPI 3.0 spec for the API surface.
2) A best-effort relational SQL schema for the underlying data model.

Guidelines:
- Use OpenAPI 3.0 (YAML).
- Group endpoints under reasonable tags (e.g., Projects, Tasks).
- Infer request and response schemas from the examples.
- Include all relevant query parameters with types.
- Use sensible names for components/schemas.
- For SQL, include primary keys, foreign keys where obvious, and basic types.

Return the result in the following exact format (no explanation text):

===OPENAPI===
<OpenAPI 3 YAML here>
===SCHEMA_SQL===
<SQL schema here>
===END===

Here is the JSON with observed API calls:

""" + summaryJson + "\n";
    }

    /**
     * Prompt for generating a Spring Boot backend implementation from:
     *  - an OpenAPI spec
     *  - a SQL schema
     *
     * The LLMClient stub looks for the phrase:
     *  - "Generate Spring Boot backend code"
     * and expects the response to be split into files using:
     *  - ===FILE:relative/path===
     *  - ===END===
     */
    public static String buildBackendPrompt(String openapiYaml, String schemaSql) {
        return """
You are an expert Java backend engineer.

Generate Spring Boot backend code
for the following API and database schema.

Requirements:
- Use Java 17 and Spring Boot 3.
- Use layered architecture: controller, service, repository, entity (JPA).
- Implement REST endpoints that match the OpenAPI spec.
- Use JPA entities that match the provided SQL schema.
- Use standard Spring Boot annotations (@RestController, @Service, @Entity, etc.).
- Use constructor injection where possible.
- Do not include any explanation comments outside of the code.

Project structure to generate:
- pom.xml
- src/main/java/com/clooney/generated/Application.java
- src/main/java/com/clooney/generated/controller/...Controller.java
- src/main/java/com/clooney/generated/service/...Service.java
- src/main/java/com/clooney/generated/repository/...Repository.java
- src/main/java/com/clooney/generated/entity/...Entity.java
- src/main/resources/application.yml (basic DB config placeholder)

Return all files in the following exact format:

===FILE:backend/generated/java-backend/pom.xml===
<pom.xml content>
===FILE:backend/generated/java-backend/src/main/java/com/clooney/generated/Application.java===
<Java code>
===FILE:backend/generated/java-backend/src/main/java/.../SomeController.java===
<Java code>
...
===END===

Here is the OpenAPI spec:

""" + openapiYaml + """

Here is the SQL schema:

""" + schemaSql + "\n";
    }

    /**
     * Prompt for generating RestAssured-based JUnit tests from OpenAPI.
     *
     * The LLMClient stub looks for the phrase:
     *  - "Generate JUnit tests using RestAssured"
     * and expects the response with:
     *  - ===FILE:tests/backend/...===
     *  - ===END===
     */
    public static String buildTestsPrompt(String openapiYaml) {
        return """
You are an expert in API testing with JUnit 5 and RestAssured.

Generate JUnit tests using RestAssured
for the following REST API defined in OpenAPI.

Requirements:
- Use JUnit 5.
- Use RestAssured for HTTP calls.
- Group tests into reasonable test classes (e.g., ProjectsApiTests, TasksApiTests).
- For each endpoint, include:
  - A happy-path test.
  - At least one edge-case / negative test if appropriate.
- Make the base URI configurable at the top of each test class (localhost:8080 by default).
- Do not include any explanation text; only code.

Return the tests in the following format:

===FILE:tests/backend/SomeApiTests.java===
<Java test code>
===FILE:tests/backend/AnotherApiTests.java===
<Java test code>
===END===

Here is the OpenAPI spec:

""" + openapiYaml + "\n";
    }
}
