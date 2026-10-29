package com.codecart.coffee.tests.coffees;

import com.codecart.coffee.base.BaseTest;
import com.codecart.coffee.clients.coffees.CoffeeApi;
import com.codecart.coffee.data.CoffeeFactory;
import com.codecart.coffee.mocks.CoffeeStubs;
import com.codecart.coffee.models.coffees.Coffee;
import com.codecart.coffee.utils.Tokens;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static org.testng.Assert.assertEquals;

public class UpdateCoffeeTests extends BaseTest {

    private final CoffeeApi coffeeApi = new CoffeeApi();

    // ---------- Valid update ----------

    @DataProvider(name = "existingIds")
    public Object[][] existingIds() {
        return new Object[][]{
                {"1"},
                {"2"}
        };
    }

    @Test(dataProvider = "existingIds", groups = {"smoke", "regression"})
    public void updateCoffee_withValidData_returns200(String id) {
        CoffeeStubs.updateCoffee(wireMockServer);
        Coffee request = CoffeeFactory.validCoffee();

        Response response = coffeeApi.updateCoffee(Tokens.ADMIN, id, request);

        assertEquals(response.statusCode(), 200, "Status mismatch for id: " + id);

        Coffee actual = response.as(Coffee.class);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(String.valueOf(actual.getId()), id, "Id mismatch");
        softAssert.assertEquals(actual.getName(), request.getName(), "Name mismatch for id: " + id);
        softAssert.assertEquals(actual.getType(), request.getType(), "Type mismatch for id: " + id);
        softAssert.assertEquals(actual.getPrice(), request.getPrice(), 0.001, "Price mismatch for id: " + id);
        softAssert.assertEquals(actual.getStock(), request.getStock(), "Stock mismatch for id: " + id);
        softAssert.assertAll();
    }

    // ---------- Invalid body (all 4 fields are required) ----------

    @DataProvider(name = "invalidCoffees")
    public Object[][] invalidCoffees() {
        return new Object[][]{
                {"name is empty", CoffeeFactory.withName("")},
                {"name has 51 characters", CoffeeFactory.withName("A".repeat(51))},
                {"price is 0", CoffeeFactory.withPrice(0.0)},
                {"price is 100.01", CoffeeFactory.withPrice(100.01)},
                {"stock is -1", CoffeeFactory.withStock(-1)},
                {"type is TEA", CoffeeFactory.withType("TEA")},
                {"name is missing", CoffeeFactory.withoutName()},
                {"type is missing", CoffeeFactory.withoutType()},
                {"price is missing", CoffeeFactory.withoutPrice()},
                {"stock is missing", CoffeeFactory.withoutStock()}
        };
    }

    @Test(dataProvider = "invalidCoffees", groups = {"regression"})
    public void updateCoffee_withInvalidData_returns400(String caseName, Coffee request) {
        CoffeeStubs.updateCoffee(wireMockServer);

        Response response = coffeeApi.updateCoffee(Tokens.ADMIN, "1", request);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 400, "Status mismatch for: " + caseName);
        softAssert.assertEquals(response.jsonPath().getString("code"), "VALIDATION_ERROR",
                "Error code mismatch for: " + caseName);
        softAssert.assertAll();
    }

    // ---------- Not found ----------

    @Test(groups = {"regression"})
    public void updateCoffee_withUnknownId_returns404() {
        CoffeeStubs.updateCoffee(wireMockServer);

        Response response = coffeeApi.updateCoffee(Tokens.ADMIN, "999", CoffeeFactory.validCoffee());

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 404, "Status mismatch");
        softAssert.assertEquals(response.jsonPath().getString("code"), "COFFEE_NOT_FOUND", "Error code mismatch");
        softAssert.assertAll();
    }

    // ---------- Customer ----------

    @Test(groups = {"regression"})
    public void updateCoffee_asCustomer_returns403() {
        CoffeeStubs.updateCoffee(wireMockServer);

        Response response = coffeeApi.updateCoffee(Tokens.CUSTOMER, "1", CoffeeFactory.validCoffee());

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
    public void updateCoffee_withBadToken_returns401(String caseName, String token, String expectedCode) {
        Response response = coffeeApi.updateCoffee(token, "1", CoffeeFactory.validCoffee());

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 401, "Status mismatch for: " + caseName);
        softAssert.assertEquals(response.jsonPath().getString("code"), expectedCode,
                "Error code mismatch for: " + caseName);
        softAssert.assertAll();
    }
}