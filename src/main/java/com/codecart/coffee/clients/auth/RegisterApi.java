package com.codecart.coffee.clients.auth;

import static io.restassured.RestAssured.given;

import com.codecart.coffee.models.auth.register.RegisterRequest;
import io.restassured.response.Response;

public class RegisterApi {

    public Response register(RegisterRequest registerRequest) {
        return given()
                .header("Content-Type", "application/json")
                .body(registerRequest)
                .when()
                .post("/auth/register");

    }
}
