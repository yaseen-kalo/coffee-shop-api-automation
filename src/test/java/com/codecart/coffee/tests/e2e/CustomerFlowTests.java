package com.codecart.coffee.tests.e2e;

import com.codecart.coffee.clients.coffees.CoffeeApi;
import com.codecart.coffee.clients.orders.OrderApi;
import com.codecart.coffee.data.LoginFactory;
import com.codecart.coffee.data.OrderFactory;
import com.codecart.coffee.mocks.CoffeeStubs;
import com.codecart.coffee.mocks.FlowStubs;
import com.codecart.coffee.mocks.LoginStubs;
import com.codecart.coffee.models.coffees.Coffee;
import com.codecart.coffee.models.orders.Order;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class CustomerFlowTests extends E2EBase {

    private final CoffeeApi coffeeApi = new CoffeeApi();
    private final OrderApi orderApi = new OrderApi();

    /**
     * Customer flow: login, see the menu, see one coffee, place an order,
     * read the order, cancel it, and read it again.
     */
    @Test(groups = {"smoke", "e2e", "regression"})
    public void customerFlow_browseOrderAndCancel() {
        LoginStubs.login(wireMockServer);
        CoffeeStubs.getCoffees(wireMockServer);
        CoffeeStubs.getCoffeeById(wireMockServer);
        FlowStubs.orderFlow(wireMockServer);

        String token = loginAndGetToken(LoginFactory.validLogin());

        // Step 1: see the menu
        Response menu = coffeeApi.getCoffees(token, null);
        assertEquals(menu.statusCode(), 200, "Step 1 (menu): status mismatch");
        assertEquals(menu.as(Coffee[].class).length, 8, "Step 1 (menu): number of coffees mismatch");

        // Step 2: see one coffee
        Response coffee = coffeeApi.getCoffeeById(token, "3");
        assertEquals(coffee.statusCode(), 200, "Step 2 (one coffee): status mismatch");
        assertEquals(coffee.as(Coffee.class).getName(), "Cappuccino", "Step 2 (one coffee): name mismatch");

        // Step 3: place an order
        Response placed = orderApi.placeOrder(token, OrderFactory.validOrder());
        assertEquals(placed.statusCode(), 201, "Step 3 (place): status mismatch");
        Order placedOrder = placed.as(Order.class);
        assertEquals(placedOrder.getStatus(), "PLACED", "Step 3 (place): status field mismatch");
        String id = String.valueOf(placedOrder.getId());

        // Step 4: read the order
        Response fetched = orderApi.getOrderById(token, id);
        assertEquals(fetched.statusCode(), 200, "Step 4 (read order): status mismatch");
        assertEquals(fetched.as(Order.class).getStatus(), "PLACED", "Step 4 (read order): status field mismatch");

        // Step 5: cancel it
        Response cancelled = orderApi.cancelOrder(token, id);
        assertEquals(cancelled.statusCode(), 200, "Step 5 (cancel): status mismatch");
        assertEquals(cancelled.as(Order.class).getStatus(), "CANCELLED", "Step 5 (cancel): status field mismatch");

        // Step 6: read it again, it must show CANCELLED
        Response again = orderApi.getOrderById(token, id);
        assertEquals(again.statusCode(), 200, "Step 6 (read again): status mismatch");
        assertEquals(again.as(Order.class).getStatus(), "CANCELLED", "Step 6 (read again): status field mismatch");
    }
}