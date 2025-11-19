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

    @Test
    void deleteNonExistingProject_returns404ish() {
        given()
            .accept(ContentType.JSON)
        .when()
            .delete("/projects/does-not-exist")
        .then()
            .statusCode(anyOf(is(404), is(400), is(204)));
    }
}