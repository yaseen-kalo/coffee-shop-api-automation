package com.codecart.coffee.tests.orders;

import com.codecart.coffee.base.BaseTest;
import com.codecart.coffee.clients.orders.OrderApi;
import com.codecart.coffee.data.AuthDataProviders;
import com.codecart.coffee.data.OrderFactory;
import com.codecart.coffee.mocks.OrderStubs;
import com.codecart.coffee.models.orders.Order;
import com.codecart.coffee.utils.Tokens;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.testng.Assert.assertEquals;

public class PlaceOrderTests extends BaseTest {

    private final OrderApi orderApi = new OrderApi();

    // ---------- Valid orders and total ----------

    @DataProvider(name = "validOrders")
    public Object[][] validOrders() {
        return new Object[][]{
                {"one line", OrderFactory.validOrder(), 9.00},
                {"two lines (contract example)", OrderFactory.multiLineOrder(), 12.20}
        };
    }

    @Test(dataProvider = "validOrders", groups = {"smoke", "regression"})
    public void placeOrder_withValidOrder_returns201AndTotal(String caseName, Order request,
                                                             double expectedTotal) {
        OrderStubs.placeOrder(wireMockServer);

        Response response = orderApi.placeOrder(Tokens.CUSTOMER, request);

        assertEquals(response.statusCode(), 201, "Status mismatch for: " + caseName);

        Order actual = response.as(Order.class);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertNotNull(actual.getId(), "Id should be set by the server");
        softAssert.assertEquals(actual.getStatus(), "PLACED", "Status mismatch for: " + caseName);
        softAssert.assertEquals(actual.getItems().size(), request.getItems().size(),
                "Number of lines mismatch for: " + caseName);
        softAssert.assertEquals(actual.getTotal(), expectedTotal, 0.001, "Total mismatch for: " + caseName);
        softAssert.assertAll();
    }

    // ---------- Quantity boundaries ----------

    @DataProvider(name = "quantities")
    public Object[][] quantities() {
        return new Object[][]{
                {0, 400},
                {1, 201},
                {10, 201},
                {11, 400}
        };
    }

    @Test(dataProvider = "quantities", groups = {"regression"})
    public void placeOrder_withQuantity_returnsExpectedStatus(int quantity, int expectedStatus) {
        OrderStubs.placeOrder(wireMockServer);

        Response response = orderApi.placeOrder(Tokens.CUSTOMER, OrderFactory.withQuantity(quantity));

        assertEquals(response.statusCode(), expectedStatus, "Status mismatch for quantity: " + quantity);
    }

    // ---------- Line count boundaries ----------

    @DataProvider(name = "lineCounts")
    public Object[][] lineCounts() {
        return new Object[][]{
                {0, 400},
                {1, 201},
                {5, 201},
                {6, 400}
        };
    }

    @Test(dataProvider = "lineCounts", groups = {"regression"})
    public void placeOrder_withLineCount_returnsExpectedStatus(int lines, int expectedStatus) {
        OrderStubs.placeOrder(wireMockServer);

        Response response = orderApi.placeOrder(Tokens.CUSTOMER, OrderFactory.withLines(lines));

        assertEquals(response.statusCode(), expectedStatus, "Status mismatch for lines: " + lines);
    }

    // ---------- Missing items ----------

    @Test(groups = {"regression"})
    public void placeOrder_withoutItems_returns400() {
        OrderStubs.placeOrder(wireMockServer);

        Response response = orderApi.placeOrder(Tokens.CUSTOMER, OrderFactory.withoutItems());

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 400, "Status mismatch");
        softAssert.assertEquals(response.jsonPath().getString("code"), "VALIDATION_ERROR", "Error code mismatch");
        softAssert.assertAll();
    }

    // ---------- Unknown coffee ----------

    @Test(groups = {"regression"})
    public void placeOrder_withUnknownCoffee_returns404() {
        OrderStubs.placeOrder(wireMockServer);

        Response response = orderApi.placeOrder(Tokens.CUSTOMER, OrderFactory.withCoffeeId(999));

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 404, "Status mismatch");
        softAssert.assertEquals(response.jsonPath().getString("code"), "COFFEE_NOT_FOUND", "Error code mismatch");
        softAssert.assertAll();
    }

    // ---------- Out of stock ----------

    @Test(groups = {"regression"})
    public void placeOrder_withOutOfStockCoffee_returns409() {
        OrderStubs.placeOrder(wireMockServer);
        OrderStubs.placeOrderOutOfStock(wireMockServer);

        Response response = orderApi.placeOrder(Tokens.CUSTOMER, OrderFactory.withCoffeeId(8));

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 409, "Status mismatch");
        softAssert.assertEquals(response.jsonPath().getString("code"), "OUT_OF_STOCK", "Error code mismatch");
        softAssert.assertAll();
    }

    // ---------- Admin ----------

    @Test(groups = {"smoke", "regression"})
    public void placeOrder_asAdmin_returns403() {
        OrderStubs.placeOrder(wireMockServer);

        Response response = orderApi.placeOrder(Tokens.ADMIN, OrderFactory.validOrder());

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 403, "Status mismatch");
        softAssert.assertEquals(response.jsonPath().getString("code"), "FORBIDDEN", "Error code mismatch");
        softAssert.assertAll();
    }

    // ---------- Bad tokens ----------


    @Test(dataProvider = "badTokens", dataProviderClass = AuthDataProviders.class, groups = {"regression"})
    public void placeOrder_withBadToken_returns401(String caseName, String token, String expectedCode) {
        Response response = orderApi.placeOrder(token, OrderFactory.validOrder());

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 401, "Status mismatch for: " + caseName);
        softAssert.assertEquals(response.jsonPath().getString("code"), expectedCode,
                "Error code mismatch for: " + caseName);
        softAssert.assertAll();
    }

    // ---------- Schema ----------

    @Test(groups = {"regression"})
    public void placeOrder_withValidOrder_matchesSchema() {
        OrderStubs.placeOrder(wireMockServer);

        Response response = orderApi.placeOrder(Tokens.CUSTOMER, OrderFactory.validOrder());

        assertEquals(response.statusCode(), 201, "Status mismatch");
        response.then().assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/order-schema.json"));
    }
}