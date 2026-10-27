package com.codecart.coffee.clients.auth;

import com.codecart.coffee.models.auth.login.LoginRequest;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class LoginApi {

    public Response login(LoginRequest loginRequest) {
        return given()
                .header("Content-Type", "application/json")
                .body(loginRequest)
                .when()
                .post("/auth/login");

    }
}
