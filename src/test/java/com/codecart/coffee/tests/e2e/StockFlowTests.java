package com.codecart.coffee.tests.e2e;

import com.codecart.coffee.clients.coffees.CoffeeApi;
import com.codecart.coffee.clients.orders.OrderApi;
import com.codecart.coffee.data.LoginFactory;
import com.codecart.coffee.data.OrderFactory;
import com.codecart.coffee.mocks.FlowStubs;
import com.codecart.coffee.mocks.LoginStubs;
import com.codecart.coffee.models.coffees.Coffee;
import com.codecart.coffee.models.orders.Order;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static org.testng.Assert.assertEquals;

public class StockFlowTests extends E2EBase {

    private final CoffeeApi coffeeApi = new CoffeeApi();
    private final OrderApi orderApi = new OrderApi();

    /**
     * Stock flow: stock is set to 2, an order for 2 works, one more is refused (409),
     * the admin adds stock, and ordering works again.
     */
    @Test(groups = {"e2e", "regression"})
    public void stockFlow_runsOutAndRecovers() {
        LoginStubs.login(wireMockServer);
        FlowStubs.stockFlow(wireMockServer);

        String adminToken = loginAndGetToken(LoginFactory.validAdminLogin());
        String customerToken = loginAndGetToken(LoginFactory.validLogin());

        // Step 1: admin sets the stock of coffee 1 to 2
        Response lowStock = coffeeApi.updateStock(adminToken, "1", 2);
        assertEquals(lowStock.statusCode(), 200, "Step 1 (set stock 2): status mismatch");
        assertEquals(lowStock.as(Coffee.class).getStock(), Integer.valueOf(2), "Step 1 (set stock 2): stock mismatch");

        // Step 2: customer orders 2, this uses all the stock
        Response first = orderApi.placeOrder(customerToken, OrderFactory.withCoffeeAndQuantity(1, 2));
        assertEquals(first.statusCode(), 201, "Step 2 (order 2): status mismatch");
        assertEquals(first.as(Order.class).getTotal(), 6.00, 0.001, "Step 2 (order 2): total mismatch");

        // Step 3: customer orders 1 more, there is no stock left
        Response refused = orderApi.placeOrder(customerToken, OrderFactory.withCoffeeAndQuantity(1, 1));
        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(refused.statusCode(), 409, "Step 3 (order 1 more): status mismatch");
        softAssert.assertEquals(refused.jsonPath().getString("code"), "OUT_OF_STOCK",
                "Step 3 (order 1 more): error code mismatch");
        softAssert.assertAll();

        // Step 4: admin adds stock
        Response restocked = orderApiRestock(adminToken);
        assertEquals(restocked.statusCode(), 200, "Step 4 (add stock): status mismatch");
        assertEquals(restocked.as(Coffee.class).getStock(), Integer.valueOf(10), "Step 4 (add stock): stock mismatch");

        // Step 5: ordering works again
        Response again = orderApi.placeOrder(customerToken, OrderFactory.withCoffeeAndQuantity(1, 1));
        assertEquals(again.statusCode(), 201, "Step 5 (order again): status mismatch");
        assertEquals(again.as(Order.class).getTotal(), 3.00, 0.001, "Step 5 (order again): total mismatch");
    }

    private Response orderApiRestock(String adminToken) {
        return coffeeApi.updateStock(adminToken, "1", 10);
    }
}