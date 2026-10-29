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

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.testng.Assert.assertEquals;

public class GetCoffeeByIdTests extends BaseTest {

    private final CoffeeApi coffeeApi = new CoffeeApi();

    // ---------- Positive ----------

    @DataProvider(name = "existingCoffees")
    public Object[][] existingCoffees() {
        return new Object[][]{
                {"1", "Espresso", 3.00},
                {"2", "Macchiato", 3.50},
                {"3", "Cappuccino", 4.50}
        };
    }

    @Test(dataProvider = "existingCoffees", groups = {"smoke", "regression"})
    public void getCoffeeById_withExistingId_returns200(String id, String expectedName, double expectedPrice) {
        CoffeeStubs.getCoffeeById(wireMockServer);

        Response response = coffeeApi.getCoffeeById(Tokens.CUSTOMER, id);

        assertEquals(response.statusCode(), 200, "Status mismatch for id: " + id);

        Coffee actual = response.as(Coffee.class);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(actual.getId().toString(), id, "Id mismatch");
        softAssert.assertEquals(actual.getName(), expectedName, "Name mismatch for id: " + id);
        softAssert.assertEquals(actual.getPrice(), expectedPrice, 0.001, "Price mismatch for id: " + id);
        softAssert.assertAll();
    }

    // ---------- Schema ----------

    @Test(groups = {"regression"})
    public void getCoffeeById_matchesSchema() {
        CoffeeStubs.getCoffeeById(wireMockServer);

        Response response = coffeeApi.getCoffeeById(Tokens.ADMIN, "1");

        assertEquals(response.statusCode(), 200, "Status mismatch");
        response.then().assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/coffee-schema.json"));
    }

    // ---------- Not found ----------

    @Test(groups = {"regression"})
    public void getCoffeeById_withUnknownId_returns404() {
        CoffeeStubs.getCoffeeById(wireMockServer);

        Response response = coffeeApi.getCoffeeById(Tokens.CUSTOMER, "999");

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 404, "Status mismatch");
        softAssert.assertEquals(response.jsonPath().getString("code"), "COFFEE_NOT_FOUND", "Error code mismatch");
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
    public void getCoffeeById_withBadToken_returns401(String caseName, String token, String expectedCode) {
        Response response = coffeeApi.getCoffeeById(token, "1");

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 401, "Status mismatch for: " + caseName);
        softAssert.assertEquals(response.jsonPath().getString("code"), expectedCode,
                "Error code mismatch for: " + caseName);
        softAssert.assertAll();
    }

    // ---------- Edge ids ----------

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
    public void getCoffeeById_withInvalidId_returns400(String id) {
        CoffeeStubs.getCoffeeById(wireMockServer);

        Response response = coffeeApi.getCoffeeById(Tokens.CUSTOMER, id);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 400, "Status mismatch for id: " + id);
        softAssert.assertEquals(response.jsonPath().getString("code"), "INVALID_ID",
                "Error code mismatch for id: " + id);
        softAssert.assertAll();
    }
}