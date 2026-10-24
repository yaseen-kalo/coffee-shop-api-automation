package com.codecart.coffee.tests;

import com.codecart.coffee.base.BaseTest;
import com.codecart.coffee.utils.Tokens;
import static io.restassured.RestAssured.given;
import static org.testng.Assert.assertEquals;

import io.restassured.response.Response;
import org.testng.annotations.Test;

public class SetupTest extends BaseTest {

    @Test
    public void testWithExpiredToken() {

        Response response  = given()
                .header("Authorization", "Bearer " + Tokens.EXPIRED )
                .when()
                .get("/coffees");

        String body = response.jsonPath().getString("code");
        assertEquals(body, "TOKEN_EXPIRED");
    }

    @Test
    public void testWithInvalidURL() {

        Response response  = given()
                .header("Authorization", "Bearer " + Tokens.ADMIN )
                .when()
                .get("/invalidURL");

        String body = response.jsonPath().getString("code");
        assertEquals(body, "ROUTE_NOT_FOUND");
    }

    @Test
    public void testWithWithOutToken() {

        Response response  = given()
                .when()
                .get("/coffees");

        String body = response.jsonPath().getString("code");
        assertEquals(body, "INVALID_TOKEN");
    }
}
