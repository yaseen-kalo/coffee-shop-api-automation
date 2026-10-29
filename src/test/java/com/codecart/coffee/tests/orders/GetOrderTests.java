package com.codecart.coffee.tests.orders;

import com.codecart.coffee.base.BaseTest;
import com.codecart.coffee.clients.orders.OrderApi;
import com.codecart.coffee.data.AuthDataProviders;
import com.codecart.coffee.mocks.OrderStubs;
import com.codecart.coffee.models.orders.Order;
import com.codecart.coffee.utils.Tokens;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.testng.Assert.assertEquals;

public class GetOrderTests extends BaseTest {

    private final OrderApi orderApi = new OrderApi();

    // ---------- GET /orders ----------

    @Test(groups = {"smoke", "regression"})
    public void getOrders_asCustomer_returnsList() {
        OrderStubs.getOrders(wireMockServer);

        Response response = orderApi.getOrders(Tokens.CUSTOMER);

        assertEquals(response.statusCode(), 200, "Status mismatch");

        Order[] orders = response.as(Order[].class);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(orders.length, 3, "Number of orders mismatch");
        softAssert.assertEquals(orders[0].getStatus(), "PLACED", "First order status mismatch");
        softAssert.assertAll();
    }

    @Test(groups = {"regression"})
    public void getOrders_withNoOrders_returnsEmptyList() {
        OrderStubs.getOrdersEmpty(wireMockServer);

        Response response = orderApi.getOrders(Tokens.CUSTOMER);

        assertEquals(response.statusCode(), 200, "Status mismatch");
        assertEquals(response.as(Order[].class).length, 0, "List should be empty");
    }

    @Test(groups = {"regression"})
    public void getOrders_asAdmin_returns403() {
        OrderStubs.getOrders(wireMockServer);

        Response response = orderApi.getOrders(Tokens.ADMIN);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 403, "Status mismatch");
        softAssert.assertEquals(response.jsonPath().getString("code"), "FORBIDDEN", "Error code mismatch");
        softAssert.assertAll();
    }

    // ---------- GET /orders/{id} ----------

    @DataProvider(name = "existingOrders")
    public Object[][] existingOrders() {
        return new Object[][]{
                {"customer reads order 1", Tokens.CUSTOMER, "1", "PLACED"},
                {"admin reads order 2", Tokens.ADMIN, "2", "PREPARING"},
                {"customer reads order 3", Tokens.CUSTOMER, "3", "COMPLETED"}
        };
    }

    @Test(dataProvider = "existingOrders", groups = {"smoke", "regression"})
    public void getOrderById_withExistingId_returns200(String caseName, String token, String id,
                                                       String expectedStatus) {
        OrderStubs.getOrderById(wireMockServer);

        Response response = orderApi.getOrderById(token, id);

        assertEquals(response.statusCode(), 200, "Status mismatch for: " + caseName);

        Order actual = response.as(Order.class);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(String.valueOf(actual.getId()), id, "Id mismatch for: " + caseName);
        softAssert.assertEquals(actual.getStatus(), expectedStatus, "Status mismatch for: " + caseName);
        softAssert.assertAll();
    }

    /**
     * Checks that one order has the right fields and types, and no extra fields.
     */

    @Test(groups = {"regression"})
    public void getOrderById_matchesSchema() {
        OrderStubs.getOrderById(wireMockServer);

        Response response = orderApi.getOrderById(Tokens.CUSTOMER, "1");

        assertEquals(response.statusCode(), 200, "Status mismatch");
        response.then().assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/order-schema.json"));
    }

    @Test(groups = {"regression"})
    public void getOrderById_withUnknownId_returns404() {
        OrderStubs.getOrderById(wireMockServer);

        Response response = orderApi.getOrderById(Tokens.CUSTOMER, "999");

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 404, "Status mismatch");
        softAssert.assertEquals(response.jsonPath().getString("code"), "ORDER_NOT_FOUND", "Error code mismatch");
        softAssert.assertAll();
    }

    @DataProvider(name = "invalidIds")
    public Object[][] invalidIds() {
        return new Object[][]{
                {"0"},
                {"-1"},
                {"abc"},
                {"1.5"},
                {"99999999999"}
        };
    }

    @Test(dataProvider = "invalidIds", groups = {"regression"})
    public void getOrderById_withInvalidId_returns400(String id) {
        OrderStubs.getOrderById(wireMockServer);

        Response response = orderApi.getOrderById(Tokens.CUSTOMER, id);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 400, "Status mismatch for id: " + id);
        softAssert.assertEquals(response.jsonPath().getString("code"), "INVALID_ID",
                "Error code mismatch for id: " + id);
        softAssert.assertAll();
    }

    // ---------- Bad tokens ----------

    @Test(dataProvider = "badTokens", dataProviderClass = AuthDataProviders.class, groups = {"regression"})
    public void getOrderById_withBadToken_returns401(String caseName, String token, String expectedCode) {
        Response response = orderApi.getOrderById(token, "1");

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 401, "Status mismatch for: " + caseName);
        softAssert.assertEquals(response.jsonPath().getString("code"), expectedCode,
                "Error code mismatch for: " + caseName);
        softAssert.assertAll();
    }

    /**
     * Checks the shape of the order list: field names, types, and no extra fields.
     * Status is checked first, so an error body does not give a confusing schema failure.
     */

    @Test(groups = {"regression"})
    public void getOrders_matchesSchema() {
        OrderStubs.getOrders(wireMockServer);

        Response response = orderApi.getOrders(Tokens.CUSTOMER);

        assertEquals(response.statusCode(), 200, "Status mismatch");
        response.then().assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/order-list-schema.json"));
    }
}