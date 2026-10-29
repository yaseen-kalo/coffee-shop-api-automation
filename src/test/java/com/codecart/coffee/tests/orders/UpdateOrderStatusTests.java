package com.codecart.coffee.tests.orders;

import com.codecart.coffee.base.BaseTest;
import com.codecart.coffee.clients.orders.OrderApi;
import com.codecart.coffee.mocks.OrderStubs;
import com.codecart.coffee.models.orders.Order;
import com.codecart.coffee.utils.Tokens;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.testng.Assert.assertEquals;

public class UpdateOrderStatusTests extends BaseTest {

    private final OrderApi orderApi = new OrderApi();

    // ---------- Allowed steps ----------

    @DataProvider(name = "allowedSteps")
    public Object[][] allowedSteps() {
        return new Object[][]{
                {"1", "PREPARING"},
                {"2", "READY"}
        };
    }

    @Test(dataProvider = "allowedSteps", groups = {"smoke", "regression"})
    public void updateStatus_withAllowedStep_returns200(String id, String newStatus) {
        OrderStubs.updateStatus(wireMockServer);

        Response response = orderApi.updateStatus(Tokens.ADMIN, id, newStatus);

        assertEquals(response.statusCode(), 200, "Status mismatch for order " + id);

        Order actual = response.as(Order.class);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(String.valueOf(actual.getId()), id, "Id mismatch");
        softAssert.assertEquals(actual.getStatus(), newStatus, "Status mismatch for order " + id);
        softAssert.assertAll();
    }

    // ---------- Steps that are not allowed ----------

    @DataProvider(name = "wrongSteps")
    public Object[][] wrongSteps() {
        return new Object[][]{
                {"PLACED to READY (skips a step)", "1", "READY"},
                {"PLACED to COMPLETED (skips two steps)", "1", "COMPLETED"},
                {"COMPLETED to PREPARING (going back)", "3", "PREPARING"}
        };
    }

    @Test(dataProvider = "wrongSteps", groups = {"regression"})
    public void updateStatus_withWrongStep_returns409(String caseName, String id, String newStatus) {
        OrderStubs.updateStatus(wireMockServer);

        Response response = orderApi.updateStatus(Tokens.ADMIN, id, newStatus);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 409, "Status mismatch for: " + caseName);
        softAssert.assertEquals(response.jsonPath().getString("code"), "INVALID_STATUS_TRANSITION",
                "Error code mismatch for: " + caseName);
        softAssert.assertAll();
    }

    // ---------- Invalid status value ----------

    @DataProvider(name = "invalidStatuses")
    public Object[][] invalidStatuses() {
        return new Object[][]{
                {"FLYING"},
                {""},
                {"preparing"}
        };
    }

    @Test(dataProvider = "invalidStatuses", groups = {"regression"})
    public void updateStatus_withInvalidStatus_returns400(String status) {
        OrderStubs.updateStatus(wireMockServer);

        Response response = orderApi.updateStatus(Tokens.ADMIN, "1", status);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 400, "Status mismatch for value: '" + status + "'");
        softAssert.assertEquals(response.jsonPath().getString("code"), "VALIDATION_ERROR",
                "Error code mismatch for value: '" + status + "'");
        softAssert.assertAll();
    }

    // ---------- Not found ----------

    @Test(groups = {"regression"})
    public void updateStatus_withUnknownId_returns404() {
        OrderStubs.updateStatus(wireMockServer);

        Response response = orderApi.updateStatus(Tokens.ADMIN, "999", "PREPARING");

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 404, "Status mismatch");
        softAssert.assertEquals(response.jsonPath().getString("code"), "ORDER_NOT_FOUND", "Error code mismatch");
        softAssert.assertAll();
    }

    // ---------- Customer ----------

    @Test(groups = {"regression"})
    public void updateStatus_asCustomer_returns403() {
        OrderStubs.updateStatus(wireMockServer);

        Response response = orderApi.updateStatus(Tokens.CUSTOMER, "1", "PREPARING");

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 403, "Status mismatch");
        softAssert.assertEquals(response.jsonPath().getString("code"), "FORBIDDEN", "Error code mismatch");
        softAssert.assertAll();
    }

    // ---------- Bad tokens ----------

    @DataProvider(name = "badTokens")
    public Object[][] badTokens() {
        return new Object[][]{
                {"no token", null, "INVALID_TOKEN"},
                {"unknown token", "bad-token", "INVALID_TOKEN"},
                {"expired token", Tokens.EXPIRED, "TOKEN_EXPIRED"}
        };
    }

    @Test(dataProvider = "badTokens", groups = {"regression"})
    public void updateStatus_withBadToken_returns401(String caseName, String token, String expectedCode) {
        Response response = orderApi.updateStatus(token, "1", "PREPARING");

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 401, "Status mismatch for: " + caseName);
        softAssert.assertEquals(response.jsonPath().getString("code"), expectedCode,
                "Error code mismatch for: " + caseName);
        softAssert.assertAll();
    }

    /**
     * Checks that the order after a status change has the right shape: fields, types, no extras.
     */
    @Test(groups = {"regression"})
    public void updateStatus_matchesSchema() {
        OrderStubs.updateStatus(wireMockServer);

        Response response = orderApi.updateStatus(Tokens.ADMIN, "1", "PREPARING");

        assertEquals(response.statusCode(), 200, "Status mismatch");
        response.then().assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/order-schema.json"));
    }
}