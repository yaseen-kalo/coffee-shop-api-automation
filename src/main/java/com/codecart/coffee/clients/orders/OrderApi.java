package com.codecart.coffee.clients.orders;

import static io.restassured.RestAssured.given;

import com.codecart.coffee.models.orders.Order;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

public class OrderApi {

    public Response placeOrder(String token, Order order) {
        return withAuth(token)
                .contentType(ContentType.JSON)
                .body(order)
                .when()
                .post("/orders");
    }

    public Response getOrders(String token) {
        return withAuth(token)
                .when()
                .get("/orders");
    }

    public Response getOrderById(String token, String id) {
        return withAuth(token)
                .when()
                .get("/orders/" + id);
    }

    public Response cancelOrder(String token, String id) {
        return withAuth(token)
                .when()
                .patch("/orders/" + id + "/cancel");
    }

    public Response updateStatus(String token, String id, Object status) {
        return withAuth(token)
                .contentType(ContentType.JSON)
                .body(Map.of("status", status))
                .when()
                .patch("/orders/" + id + "/status");
    }

    private RequestSpecification withAuth(String token) {
        RequestSpecification request = given();

        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }

        return request;
    }
}