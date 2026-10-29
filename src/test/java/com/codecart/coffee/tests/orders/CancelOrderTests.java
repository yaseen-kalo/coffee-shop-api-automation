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

public class CancelOrderTests extends BaseTest {

    private final OrderApi orderApi = new OrderApi();

    @Test(groups = {"smoke", "regression"})
    public void cancelOrder_whenPlaced_returns200AndCancelled() {
        OrderStubs.cancelOrder(wireMockServer);

        Response response = orderApi.cancelOrder(Tokens.CUSTOMER, "1");

        assertEquals(response.statusCode(), 200, "Status mismatch");

        Order actual = response.as(Order.class);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(actual.getId(), Integer.valueOf(1), "Id mismatch");
        softAssert.assertEquals(actual.getStatus(), "CANCELLED", "Status mismatch");
        softAssert.assertAll();
    }

    @DataProvider(name = "notCancellableOrders")
    public Object[][] notCancellableOrders() {
        return new Object[][]{
                {"order is PREPARING", "2"},
                {"order is COMPLETED", "3"}
        };
    }

    @Test(dataProvider = "notCancellableOrders", groups = {"regression"})
    public void cancelOrder_whenNotPlaced_returns409(String caseName, String id) {
        OrderStubs.cancelOrder(wireMockServer);

        Response response = orderApi.cancelOrder(Tokens.CUSTOMER, id);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 409, "Status mismatch for: " + caseName);
        softAssert.assertEquals(response.jsonPath().getString("code"), "CANNOT_CANCEL",
                "Error code mismatch for: " + caseName);
        softAssert.assertAll();
    }

    @Test(groups = {"regression"})
    public void cancelOrder_withUnknownId_returns404() {
        OrderStubs.cancelOrder(wireMockServer);

        Response response = orderApi.cancelOrder(Tokens.CUSTOMER, "999");

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 404, "Status mismatch");
        softAssert.assertEquals(response.jsonPath().getString("code"), "ORDER_NOT_FOUND", "Error code mismatch");
        softAssert.assertAll();
    }

    @Test(groups = {"regression"})
    public void cancelOrder_asAdmin_returns403() {
        OrderStubs.cancelOrder(wireMockServer);

        Response response = orderApi.cancelOrder(Tokens.ADMIN, "1");

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 403, "Status mismatch");
        softAssert.assertEquals(response.jsonPath().getString("code"), "FORBIDDEN", "Error code mismatch");
        softAssert.assertAll();
    }

    @Test(dataProvider = "badTokens", dataProviderClass = AuthDataProviders.class, groups = {"regression"})
    public void cancelOrder_withBadToken_returns401(String caseName, String token, String expectedCode) {
        Response response = orderApi.cancelOrder(token, "1");

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 401, "Status mismatch for: " + caseName);
        softAssert.assertEquals(response.jsonPath().getString("code"), expectedCode,
                "Error code mismatch for: " + caseName);
        softAssert.assertAll();
    }

    /**
     * Checks that the cancelled order still has the right shape: fields, types, no extras.
     */
    @Test(groups = {"regression"})
    public void cancelOrder_matchesSchema() {
        OrderStubs.cancelOrder(wireMockServer);

        Response response = orderApi.cancelOrder(Tokens.CUSTOMER, "1");

        assertEquals(response.statusCode(), 200, "Status mismatch");
        response.then().assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/order-schema.json"));
    }
}