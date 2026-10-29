package com.codecart.coffee.tests.e2e;

import com.codecart.coffee.clients.auth.LoginApi;
import com.codecart.coffee.clients.auth.RegisterApi;
import com.codecart.coffee.clients.coffees.CoffeeApi;
import com.codecart.coffee.clients.orders.OrderApi;
import com.codecart.coffee.data.LoginFactory;
import com.codecart.coffee.data.OrderFactory;
import com.codecart.coffee.data.RegisterFactory;
import com.codecart.coffee.mocks.CoffeeStubs;
import com.codecart.coffee.mocks.FlowStubs;
import com.codecart.coffee.models.auth.login.LoginResponse;
import com.codecart.coffee.models.auth.register.RegisterRequest;
import com.codecart.coffee.models.coffees.Coffee;
import com.codecart.coffee.models.orders.Order;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class RegisterLoginOrderFlowTests extends E2EBase {

    private final RegisterApi registerApi = new RegisterApi();
    private final LoginApi loginApi = new LoginApi();
    private final CoffeeApi coffeeApi = new CoffeeApi();
    private final OrderApi orderApi = new OrderApi();

    /**
     * A new user registers, logs in, browses the menu and orders.
     * Login must fail before the register step and work after it.
     */
    @Test(groups = {"e2e", "regression"})
    public void registerLoginOrderFlow_newUserCanOrder() {
        RegisterRequest user = RegisterFactory.withName("Flow User");
        FlowStubs.registerThenLogin(wireMockServer, user.getName(), user.getEmail(), user.getPassword());
        CoffeeStubs.getCoffees(wireMockServer);
        CoffeeStubs.getCoffeeById(wireMockServer);
        FlowStubs.orderFlow(wireMockServer);

        // Step 1: login before register must fail
        Response early = loginApi.login(LoginFactory.withCredentials(user.getEmail(), user.getPassword()));
        assertEquals(early.statusCode(), 401, "Step 1 (login before register): status mismatch");
        assertEquals(early.jsonPath().getString("code"), "INVALID_CREDENTIALS",
                "Step 1 (login before register): error code mismatch");

        // Step 2: register
        Response registered = registerApi.register(user);
        assertEquals(registered.statusCode(), 201, "Step 2 (register): status mismatch");

        // Step 3: login now works, and gives a token
        Response login = loginApi.login(LoginFactory.withCredentials(user.getEmail(), user.getPassword()));
        assertEquals(login.statusCode(), 200, "Step 3 (login): status mismatch");
        LoginResponse loginBody = login.as(LoginResponse.class);
        assertEquals(loginBody.getRole(), "CUSTOMER", "Step 3 (login): role mismatch");
        String token = loginBody.getToken();

        // Step 4: browse the menu
        Response menu = coffeeApi.getCoffees(token, null);
        assertEquals(menu.statusCode(), 200, "Step 4 (menu): status mismatch");
        assertEquals(menu.as(Coffee[].class).length, 8, "Step 4 (menu): number of coffees mismatch");

        // Step 5: order
        Response placed = orderApi.placeOrder(token, OrderFactory.validOrder());
        assertEquals(placed.statusCode(), 201, "Step 5 (order): status mismatch");
        assertEquals(placed.as(Order.class).getStatus(), "PLACED", "Step 5 (order): status field mismatch");
    }
}