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