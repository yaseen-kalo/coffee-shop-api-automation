package com.codecart.coffee.tests.coffees;

import com.codecart.coffee.base.BaseTest;
import com.codecart.coffee.clients.coffees.CoffeeApi;
import com.codecart.coffee.mocks.CoffeeStubs;
import com.codecart.coffee.utils.Tokens;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static org.testng.Assert.assertEquals;

public class DeleteCoffeeTests extends BaseTest {

    private final CoffeeApi coffeeApi = new CoffeeApi();

    // ---------- Valid delete ----------

    @DataProvider(name = "existingIds")
    public Object[][] existingIds() {
        return new Object[][]{
                {"1"},
                {"2"}
        };
    }

    @Test(dataProvider = "existingIds", groups = {"smoke", "regression"})
    public void deleteCoffee_withExistingId_returns204(String id) {
        CoffeeStubs.deleteCoffee(wireMockServer);

        Response response = coffeeApi.deleteCoffee(Tokens.ADMIN, id);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 204, "Status mismatch for id: " + id);
        softAssert.assertTrue(response.asString().isEmpty(), "Body should be empty for id: " + id);
        softAssert.assertAll();
    }

    // ---------- Not found ----------

    @Test(groups = {"regression"})
    public void deleteCoffee_withUnknownId_returns404() {
        CoffeeStubs.deleteCoffee(wireMockServer);

        Response response = coffeeApi.deleteCoffee(Tokens.ADMIN, "999");

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 404, "Status mismatch");
        softAssert.assertEquals(response.jsonPath().getString("code"), "COFFEE_NOT_FOUND", "Error code mismatch");
        softAssert.assertAll();
    }

    // ---------- Customer ----------

    @Test(groups = {"regression"})
    public void deleteCoffee_asCustomer_returns403() {
        CoffeeStubs.deleteCoffee(wireMockServer);

        Response response = coffeeApi.deleteCoffee(Tokens.CUSTOMER, "1");

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
    public void deleteCoffee_withBadToken_returns401(String caseName, String token, String expectedCode) {
        Response response = coffeeApi.deleteCoffee(token, "1");

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 401, "Status mismatch for: " + caseName);
        softAssert.assertEquals(response.jsonPath().getString("code"), expectedCode,
                "Error code mismatch for: " + caseName);
        softAssert.assertAll();
    }
}