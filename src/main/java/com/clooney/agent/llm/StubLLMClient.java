package com.clooney.agent.llm;

/**
 * Stub implementation of LLMClient.
 *
 * This is deterministic and never calls the network.
 * Used for local dev and deterministic evaluation.
 */
public class StubLLMClient implements LLMClient {

    private final String apiKey;
    private final String model;

    public StubLLMClient(String apiKey, String model) {
        this.apiKey = apiKey; // kept for symmetry, not used
        this.model = model;
    }

    @Override
    public String complete(String prompt) {
        // ===== SPEC SYNTH (OpenAPI + schema) =====
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
                      gid VARCHAR(64) PRIMARY KEY,
                      name TEXT NOT NULL
                    );

                    CREATE TABLE tasks (
                      gid VARCHAR(64) PRIMARY KEY,
                      name TEXT NOT NULL
                    );
                    ===END===
                    """;
        }

        // ===== BACKEND SYNTH (Spring Boot) =====
        if (prompt.contains("Generate Spring Boot backend code")) {
            return """
===FILE:pom.xml===
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
        <relativePath/>
    </parent>

    <groupId>com.clooney.generated</groupId>
    <artifactId>asana-clone-backend</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>Asana Clone Backend (Generated)</name>
    <description>Generated Spring Boot backend for Asana-style API</description>

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
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
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

===FILE:src/main/java/com/clooney/generated/Application.java===
package com.clooney.generated;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

===FILE:src/main/java/com/clooney/generated/entity/ProjectEntity.java===
package com.clooney.generated.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String gid;

    private String name;

    public ProjectEntity() {
    }

    public ProjectEntity(String name) {
        this.name = name;
    }

    public String getGid() {
        return gid;
    }

    public void setGid(String gid) {
        this.gid = gid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

===FILE:src/main/java/com/clooney/generated/entity/TaskEntity.java===
package com.clooney.generated.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tasks")
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String gid;

    private String name;

    public TaskEntity() {
    }

    public TaskEntity(String name) {
        this.name = name;
    }

    public String getGid() {
        return gid;
    }

    public void setGid(String gid) {
        this.gid = gid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

===FILE:src/main/java/com/clooney/generated/repository/ProjectRepository.java===
package com.clooney.generated.repository;

import com.clooney.generated.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<ProjectEntity, String> {
}

===FILE:src/main/java/com/clooney/generated/repository/TaskRepository.java===
package com.clooney.generated.repository;

import com.clooney.generated.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<TaskEntity, String> {
}

===FILE:src/main/java/com/clooney/generated/controller/AsanaCloneController.java===
package com.clooney.generated.controller;

import com.clooney.generated.entity.ProjectEntity;
import com.clooney.generated.entity.TaskEntity;
import com.clooney.generated.repository.ProjectRepository;
import com.clooney.generated.repository.TaskRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class AsanaCloneController {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public AsanaCloneController(ProjectRepository projectRepository,
                                TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;

        if (projectRepository.count() == 0) {
            projectRepository.save(new ProjectEntity("Website Revamp"));
            projectRepository.save(new ProjectEntity("Personal To-Dos"));
        }

        if (taskRepository.count() == 0) {
            taskRepository.save(new TaskEntity("Fix login bug"));
            taskRepository.save(new TaskEntity("Record math video"));
        }
    }

    @GetMapping("/projects")
    public Map<String, Object> listProjects() {
        List<ProjectEntity> projects = projectRepository.findAll();
        return Map.of("data", projects);
    }

    @GetMapping("/tasks")
    public Map<String, Object> listTasks() {
        List<TaskEntity> tasks = taskRepository.findAll();
        return Map.of("data", tasks);
    }
}

===FILE:src/main/resources/application.yml===
spring:
  datasource:
    url: jdbc:h2:mem:asana;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1
    driverClassName: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  h2:
    console:
      enabled: true

server:
  port: 8080

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
===END===
""";
        }

        // ===== TEST SYNTH (JUnit + RestAssured) =====
        if (prompt.contains("Generate JUnit tests using RestAssured")) {
            return """
===FILE:ProjectsApiTests.java===
package tests.backend;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class ProjectsApiTests {

    @BeforeAll
    static void setup() {
        String base = System.getProperty("api.baseUrl", "http://localhost:8080");
        RestAssured.baseURI = base;
    }

    @Test
    void listProjects_returns200() {
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/projects")
        .then()
            .statusCode(200)
            .body("data", notNullValue());
    }
}

===FILE:TasksApiTests.java===
package tests.backend;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class TasksApiTests {

    @BeforeAll
    static void setup() {
        String base = System.getProperty("api.baseUrl", "http://localhost:8080");
        RestAssured.baseURI = base;
    }

    @Test
    void listTasks_returns200() {
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/tasks")
        .then()
            .statusCode(200)
            .body("data", notNullValue());
    }
}
===END===
""";
        }

        // Fallback
        return "STUB_RESPONSE_FOR_PROMPT\n" + prompt + "\nEND_STUB";
    }
}
