package com.codecart.coffee.clients.coffees;

import com.codecart.coffee.models.coffees.Coffee;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

import static io.restassured.RestAssured.given;

public class CoffeeApi {

    public Response createCoffee(String token, Coffee coffee) {
        return withAuth(token)
                .contentType(ContentType.JSON)
                .body(coffee)
                .when()
                .post("/coffees");
    }

    public Response updateCoffee(String token, String id, Coffee coffee) {
        return withAuth(token)
                .contentType(ContentType.JSON)
                .body(coffee)
                .when()
                .put("/coffees/" + id);
    }

    public Response updateStock(String token, String id, Object stockValue) {
        return withAuth(token)
                .contentType(ContentType.JSON)
                .body(Map.of("stock", stockValue))
                .when()
                .patch("/coffees/" + id + "/stock");
    }

    public Response deleteCoffee(String token, String id) {
        return withAuth(token)
                .when()
                .delete("/coffees/" + id);
    }

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