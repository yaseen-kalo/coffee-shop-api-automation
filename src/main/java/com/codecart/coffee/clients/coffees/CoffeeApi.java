package com.codecart.coffee.clients.coffees;

import static io.restassured.RestAssured.given;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

public class CoffeeApi {

    public Response getCoffees(String token, String type) {
        RequestSpecification request = withAuth(token);

        if (type != null) {
            request.queryParam("type", type);
        }

        return request
                .when()
                .get("/coffees");
    }

    public Response getCoffeeById(String token, String id) {
        return withAuth(token)
                .when()
                .get("/coffees/" + id);
    }

    private RequestSpecification withAuth(String token) {
        RequestSpecification request = given();

        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }

        return request;
    }
}