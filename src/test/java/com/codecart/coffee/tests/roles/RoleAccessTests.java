package com.codecart.coffee.tests.roles;

import com.codecart.coffee.base.BaseTest;
import com.codecart.coffee.clients.auth.MeApi;
import com.codecart.coffee.clients.coffees.CoffeeApi;
import com.codecart.coffee.clients.orders.OrderApi;
import com.codecart.coffee.data.CoffeeFactory;
import com.codecart.coffee.data.OrderFactory;
import com.codecart.coffee.mocks.CoffeeStubs;
import com.codecart.coffee.mocks.MeStubs;
import com.codecart.coffee.mocks.OrderStubs;
import com.codecart.coffee.utils.Tokens;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.util.function.Function;

public class RoleAccessTests extends BaseTest {

    private final MeApi meApi = new MeApi();
    private final CoffeeApi coffeeApi = new CoffeeApi();
    private final OrderApi orderApi = new OrderApi();

    /**
     * The role matrix. Each row is one endpoint:
     * a name, the call (it takes a token), the expected status for admin, and for customer.
     */
    @DataProvider(name = "roleMatrix")
    public Object[][] roleMatrix() {
        return new Object[][]{
                row("GET /auth/me", meApi::me, 200, 200),
                row("GET /coffees", token -> coffeeApi.getCoffees(token, null), 200, 200),
                row("GET /coffees/1", token -> coffeeApi.getCoffeeById(token, "1"), 200, 200),
                row("POST /coffees", token -> coffeeApi.createCoffee(token, CoffeeFactory.validCoffee()), 201, 403),
                row("PUT /coffees/1", token -> coffeeApi.updateCoffee(token, "1", CoffeeFactory.validCoffee()), 200, 403),
                row("PATCH /coffees/1/stock", token -> coffeeApi.updateStock(token, "1", 25), 200, 403),
                row("DELETE /coffees/1", token -> coffeeApi.deleteCoffee(token, "1"), 204, 403),
                row("POST /orders", token -> orderApi.placeOrder(token, OrderFactory.validOrder()), 403, 201),
                row("GET /orders", orderApi::getOrders, 403, 200),
                row("GET /orders/1", token -> orderApi.getOrderById(token, "1"), 200, 200),
                row("PATCH /orders/1/cancel", token -> orderApi.cancelOrder(token, "1"), 403, 200),
                row("PATCH /orders/1/status", token -> orderApi.updateStatus(token, "1", "PREPARING"), 200, 403)
        };
    }

    // ---------- Admin and customer ----------

    @Test(dataProvider = "roleMatrix", groups = {"smoke", "regression"})
    public void roleMatrix_adminAndCustomer(String endpoint, Function<String, Response> call,
                                            int expectedAdmin, int expectedCustomer) {
        registerAllStubs();

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(call.apply(Tokens.ADMIN).statusCode(), expectedAdmin,
                "Admin status mismatch for: " + endpoint);
        softAssert.assertEquals(call.apply(Tokens.CUSTOMER).statusCode(), expectedCustomer,
                "Customer status mismatch for: " + endpoint);
        softAssert.assertAll();
    }

    // ---------- No token and expired token ----------

    @Test(dataProvider = "roleMatrix", groups = {"regression"})
    public void roleMatrix_withoutToken_returns401(String endpoint, Function<String, Response> call,
                                                   int ignoredAdmin, int ignoredCustomer) {
        registerAllStubs();

        Response response = call.apply(null);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 401, "Status mismatch for: " + endpoint);
        softAssert.assertEquals(response.jsonPath().getString("code"), "INVALID_TOKEN",
                "Error code mismatch for: " + endpoint);
        softAssert.assertAll();
    }

    @Test(dataProvider = "roleMatrix", groups = {"regression"})
    public void roleMatrix_withExpiredToken_returns401(String endpoint, Function<String, Response> call,
                                                       int ignoredAdmin, int ignoredCustomer) {
        registerAllStubs();

        Response response = call.apply(Tokens.EXPIRED);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 401, "Status mismatch for: " + endpoint);
        softAssert.assertEquals(response.jsonPath().getString("code"), "TOKEN_EXPIRED",
                "Error code mismatch for: " + endpoint);
        softAssert.assertAll();
    }

    // ---------- Helpers ----------

    /** Every endpoint in the matrix needs its stub, because BaseTest resets them before each test. */
    private void registerAllStubs() {
        MeStubs.me(wireMockServer);
        CoffeeStubs.getCoffees(wireMockServer);
        CoffeeStubs.getCoffeeById(wireMockServer);
        CoffeeStubs.createCoffee(wireMockServer);
        CoffeeStubs.updateCoffee(wireMockServer);
        CoffeeStubs.updateStock(wireMockServer);
        CoffeeStubs.deleteCoffee(wireMockServer);
        OrderStubs.placeOrder(wireMockServer);
        OrderStubs.getOrders(wireMockServer);
        OrderStubs.getOrderById(wireMockServer);
        OrderStubs.cancelOrder(wireMockServer);
        OrderStubs.updateStatus(wireMockServer);
    }

    private static Object[] row(String endpoint, Function<String, Response> call,
                                int expectedAdmin, int expectedCustomer) {
        return new Object[]{endpoint, call, expectedAdmin, expectedCustomer};
    }
}