package com.codecart.coffee.tests.e2e;

import com.codecart.coffee.clients.orders.OrderApi;
import com.codecart.coffee.data.LoginFactory;
import com.codecart.coffee.data.OrderFactory;
import com.codecart.coffee.mocks.FlowStubs;
import com.codecart.coffee.mocks.LoginStubs;
import com.codecart.coffee.models.orders.Order;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static org.testng.Assert.assertEquals;

public class OrderLifecycleTests extends E2EBase {

    private final OrderApi orderApi = new OrderApi();

    /**
     * Order lifecycle: the customer orders, the admin moves the status step by step,
     * the customer checks the status each time, and a late cancel fails with 409.
     */
    @Test(groups = {"e2e", "regression"})
    public void orderLifecycle_placedToCompleted_thenCancelFails() {
        LoginStubs.login(wireMockServer);
        FlowStubs.orderFlow(wireMockServer);

        String customerToken = loginAndGetToken(LoginFactory.validLogin());
        String adminToken = loginAndGetToken(LoginFactory.validAdminLogin());

        // Step 1: customer places the order
        Response placed = orderApi.placeOrder(customerToken, OrderFactory.validOrder());
        assertEquals(placed.statusCode(), 201, "Step 1 (place): status mismatch");
        Order placedOrder = placed.as(Order.class);
        assertEquals(placedOrder.getStatus(), "PLACED", "Step 1 (place): status field mismatch");
        String id = String.valueOf(placedOrder.getId());

        // Steps 2 to 7: admin changes the status, customer checks it
        for (String next : new String[]{"PREPARING", "READY", "COMPLETED"}) {
            Response changed = orderApi.updateStatus(adminToken, id, next);
            assertEquals(changed.statusCode(), 200, "Admin sets " + next + ": status mismatch");

            Response check = orderApi.getOrderById(customerToken, id);
            assertEquals(check.statusCode(), 200, "Customer reads order after " + next + ": status mismatch");
            assertEquals(check.as(Order.class).getStatus(), next,
                    "Customer reads order after " + next + ": status field mismatch");
        }

        // Step 8: the order is completed, so cancel must fail
        Response cancel = orderApi.cancelOrder(customerToken, id);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(cancel.statusCode(), 409, "Step 8 (cancel): status mismatch");
        softAssert.assertEquals(cancel.jsonPath().getString("code"), "CANNOT_CANCEL",
                "Step 8 (cancel): error code mismatch");
        softAssert.assertAll();
    }
}