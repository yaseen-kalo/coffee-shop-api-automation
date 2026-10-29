package com.codecart.coffee.tests.e2e;

import com.codecart.coffee.clients.coffees.CoffeeApi;
import com.codecart.coffee.clients.orders.OrderApi;
import com.codecart.coffee.data.CoffeeFactory;
import com.codecart.coffee.data.LoginFactory;
import com.codecart.coffee.mocks.CoffeeStubs;
import com.codecart.coffee.mocks.LoginStubs;
import com.codecart.coffee.mocks.OrderStubs;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class SecurityFlowTests extends E2EBase {

    private final CoffeeApi coffeeApi = new CoffeeApi();
    private final OrderApi orderApi = new OrderApi();

    /**
     * Security flow: a customer calls every admin endpoint and gets 403 each time.
     * Then the same calls without a token get 401 each time.
     */
    @Test(groups = {"smoke", "e2e", "regression"})
    public void securityFlow_customerGets403_thenNoTokenGets401() {
        LoginStubs.login(wireMockServer);
        CoffeeStubs.createCoffee(wireMockServer);
        CoffeeStubs.updateCoffee(wireMockServer);
        CoffeeStubs.updateStock(wireMockServer);
        CoffeeStubs.deleteCoffee(wireMockServer);
        OrderStubs.updateStatus(wireMockServer);

        String customerToken = loginAndGetToken(LoginFactory.validLogin());
        Map<String, Function<String, Response>> adminEndpoints = adminEndpoints();

        SoftAssert softAssert = new SoftAssert();

        // Step 1: customer token, every admin endpoint must say 403
        adminEndpoints.forEach((endpoint, call) -> {
            Response response = call.apply(customerToken);
            softAssert.assertEquals(response.statusCode(), 403, "Customer status mismatch for: " + endpoint);
            softAssert.assertEquals(response.jsonPath().getString("code"), "FORBIDDEN",
                    "Customer error code mismatch for: " + endpoint);
        });

        // Step 2: no token, every admin endpoint must say 401
        adminEndpoints.forEach((endpoint, call) -> {
            Response response = call.apply(null);
            softAssert.assertEquals(response.statusCode(), 401, "No-token status mismatch for: " + endpoint);
            softAssert.assertEquals(response.jsonPath().getString("code"), "INVALID_TOKEN",
                    "No-token error code mismatch for: " + endpoint);
        });

        softAssert.assertAll();
    }

    private Map<String, Function<String, Response>> adminEndpoints() {
        Map<String, Function<String, Response>> endpoints = new LinkedHashMap<>();
        endpoints.put("POST /coffees", token -> coffeeApi.createCoffee(token, CoffeeFactory.validCoffee()));
        endpoints.put("PUT /coffees/1", token -> coffeeApi.updateCoffee(token, "1", CoffeeFactory.validCoffee()));
        endpoints.put("PATCH /coffees/1/stock", token -> coffeeApi.updateStock(token, "1", 25));
        endpoints.put("DELETE /coffees/1", token -> coffeeApi.deleteCoffee(token, "1"));
        endpoints.put("PATCH /orders/1/status", token -> orderApi.updateStatus(token, "1", "PREPARING"));
        return endpoints;
    }
}