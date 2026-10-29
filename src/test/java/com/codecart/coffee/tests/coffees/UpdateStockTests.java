package com.codecart.coffee.tests.coffees;

import com.codecart.coffee.base.BaseTest;
import com.codecart.coffee.clients.coffees.CoffeeApi;
import com.codecart.coffee.mocks.CoffeeStubs;
import com.codecart.coffee.models.coffees.Coffee;
import com.codecart.coffee.utils.Tokens;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static org.testng.Assert.assertEquals;

public class UpdateStockTests extends BaseTest {

    private final CoffeeApi coffeeApi = new CoffeeApi();

    // ---------- Valid stock ----------

    @DataProvider(name = "validStocks")
    public Object[][] validStocks() {
        return new Object[][]{
                {0},
                {1},
                {25}
        };
    }

    @Test(dataProvider = "validStocks", groups = {"smoke", "regression"})
    public void updateStock_withValidStock_returns200(int stock) {
        CoffeeStubs.updateStock(wireMockServer);

        Response response = coffeeApi.updateStock(Tokens.ADMIN, "1", stock);

        assertEquals(response.statusCode(), 200, "Status mismatch for stock: " + stock);

        Coffee actual = response.as(Coffee.class);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(actual.getStock(), Integer.valueOf(stock), "Stock mismatch");
        softAssert.assertEquals(actual.getId(), Integer.valueOf(1), "Id mismatch");
        softAssert.assertEquals(actual.getName(), "Espresso", "Name mismatch");
        softAssert.assertAll();
    }

    // ---------- Invalid stock ----------

    @DataProvider(name = "invalidStocks")
    public Object[][] invalidStocks() {
        return new Object[][]{
                {"negative number", -1},
                {"text", "abc"}
        };
    }

    @Test(dataProvider = "invalidStocks", groups = {"regression"})
    public void updateStock_withInvalidStock_returns400(String caseName, Object stock) {
        CoffeeStubs.updateStock(wireMockServer);

        Response response = coffeeApi.updateStock(Tokens.ADMIN, "1", stock);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 400, "Status mismatch for: " + caseName);
        softAssert.assertEquals(response.jsonPath().getString("code"), "VALIDATION_ERROR",
                "Error code mismatch for: " + caseName);
        softAssert.assertAll();
    }

    // ---------- Not found ----------

    @Test(groups = {"regression"})
    public void updateStock_withUnknownId_returns404() {
        CoffeeStubs.updateStock(wireMockServer);

        Response response = coffeeApi.updateStock(Tokens.ADMIN, "999", 5);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 404, "Status mismatch");
        softAssert.assertEquals(response.jsonPath().getString("code"), "COFFEE_NOT_FOUND", "Error code mismatch");
        softAssert.assertAll();
    }

    // ---------- Customer ----------

    @Test(groups = {"regression"})
    public void updateStock_asCustomer_returns403() {
        CoffeeStubs.updateStock(wireMockServer);

        Response response = coffeeApi.updateStock(Tokens.CUSTOMER, "1", 25);

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
    public void updateStock_withBadToken_returns401(String caseName, String token, String expectedCode) {
        Response response = coffeeApi.updateStock(token, "1", 25);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 401, "Status mismatch for: " + caseName);
        softAssert.assertEquals(response.jsonPath().getString("code"), expectedCode,
                "Error code mismatch for: " + caseName);
        softAssert.assertAll();
    }
}