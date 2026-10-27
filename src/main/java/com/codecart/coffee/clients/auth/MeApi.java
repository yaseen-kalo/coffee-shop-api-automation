package com.codecart.coffee.clients.auth;

import static io.restassured.RestAssured.given;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class MeApi {

    public Response me(String token) {
        RequestSpecification request = given();

        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }

        return request
                .when()
                .get("/auth/me");
    }
}