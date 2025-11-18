package com.clooney.agent.llm;

/**
 * TEMP STUB IMPLEMENTATION for local wiring & debugging.
 *
 * This version NEVER calls the network. It just returns deterministic
 * stubbed responses that match what BackendSynthesizer / SpecSynthesizer /
 * TestSynthesizer expect.
 *
 * Once everything else is working, you can restore the real HTTP-based version.
 */
public class LLMClient {

    private final String apiKey;
    private final String model;

    public LLMClient(String apiKey, String model) {
        // Keep the constructor, but don't actually use the key for now.
        this.apiKey = apiKey;
        this.model = model;
    }

    public String complete(String prompt) {
        // SpecSynthesizer prompt
        if (prompt.contains("===OPENAPI===")) {
            return """
                    ===OPENAPI===
                    openapi: 3.0.0
                    info:
                      title: Stub Asana Clone API
                      version: 1.0.0
                    paths:
                      /projects:
                        get:
                          summary: List projects
                          responses:
                            '200':
                              description: OK
                      /tasks:
                        get:
                          summary: List tasks
                          responses:
                            '200':
                              description: OK
                    ===SCHEMA_SQL===
                    CREATE TABLE projects (
                      id SERIAL PRIMARY KEY,
                      name TEXT NOT NULL
                    );

                    CREATE TABLE tasks (
                      id SERIAL PRIMARY KEY,
                      project_id INT NOT NULL,
                      title TEXT NOT NULL,
                      completed BOOLEAN DEFAULT FALSE
                    );
                    ===END===
                    """;
        }

        // BackendSynthesizer prompt
        if (prompt.contains("Generate Spring Boot backend code")) {
            return """
        ===FILE:backend/generated/java-backend/pom.xml===
        <project ...>
          ...
          <dependencies>
            <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-web</artifactId>
            </dependency>
            <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-data-jpa</artifactId>
            </dependency>
            <!-- keep postgres if you want -->
            <dependency>
              <groupId>org.postgresql</groupId>
              <artifactId>postgresql</artifactId>
              <scope>runtime</scope>
            </dependency>
            <!-- add this H2 dep in the stub as well -->
            <dependency>
              <groupId>com.h2database</groupId>
              <artifactId>h2</artifactId>
              <scope>runtime</scope>
            </dependency>
            ...
          </dependencies>
          ...
        </project>
        ===FILE:backend/generated/java-backend/src/main/java/com/clooney/generated/Application.java===
        ...
        ===END===
        """;
        }

        // TestSynthesizer prompt
        if (prompt.contains("Generate JUnit tests using RestAssured")) {
            return """
                    ===FILE:tests/backend/ProjectApiTests.java===
                    package tests.backend;

                    import io.restassured.RestAssured;
                    import io.restassured.http.ContentType;
                    import org.junit.jupiter.api.BeforeAll;
                    import org.junit.jupiter.api.Test;

                    import static io.restassured.RestAssured.given;
                    import static org.hamcrest.Matchers.*;

                    public class ProjectApiTests {

                        @BeforeAll
                        static void setup() {
                            RestAssured.baseURI = "http://localhost:8080";
                        }

                        @Test
                        void listProjects_returns200() {
                            given()
                              .accept(ContentType.JSON)
                            .when()
                              .get("/projects")
                            .then()
                              .statusCode(anyOf(is(200), is(404)));
                        }
                    }
                    ===END===
                    """;
        }

        // Generic fallback
        return "STUB_RESPONSE_FOR_PROMPT\n" + prompt + "\nEND_STUB";
    }
}
