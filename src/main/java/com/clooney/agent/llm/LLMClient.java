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
                    <project xmlns="http://maven.apache.org/POM/4.0.0"
                             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.clooney.generated</groupId>
                      <artifactId>asana-clone-backend</artifactId>
                      <version>0.0.1-SNAPSHOT</version>
                      <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>3.3.0</version>
                        <relativePath/>
                      </parent>
                      <properties>
                        <java.version>17</java.version>
                      </properties>
                      <dependencies>
                        <dependency>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-starter-web</artifactId>
                        </dependency>
                        <dependency>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-starter-data-jpa</artifactId>
                        </dependency>
                        <dependency>
                          <groupId>org.postgresql</groupId>
                          <artifactId>postgresql</artifactId>
                          <scope>runtime</scope>
                        </dependency>
                        <dependency>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-starter-test</artifactId>
                          <scope>test</scope>
                        </dependency>
                        <dependency>
                          <groupId>io.rest-assured</groupId>
                          <artifactId>rest-assured</artifactId>
                          <scope>test</scope>
                        </dependency>
                      </dependencies>
                      <build>
                        <plugins>
                          <plugin>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-maven-plugin</artifactId>
                          </plugin>
                        </plugins>
                      </build>
                    </project>
                    ===FILE:backend/generated/java-backend/src/main/java/com/clooney/generated/Application.java===
                    package com.clooney.generated;

                    import org.springframework.boot.SpringApplication;
                    import org.springframework.boot.autoconfigure.SpringBootApplication;

                    @SpringBootApplication
                    public class Application {
                        public static void main(String[] args) {
                            SpringApplication.run(Application.class, args);
                        }
                    }
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
