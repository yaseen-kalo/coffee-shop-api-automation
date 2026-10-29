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

public class GetCoffeesTests extends BaseTest {

    private final CoffeeApi coffeeApi = new CoffeeApi();

    // ---------- Both roles can read ----------

    @DataProvider(name = "readers")
    public Object[][] readers() {
        return new Object[][]{
                {"admin", Tokens.ADMIN},
                {"customer", Tokens.CUSTOMER}
        };
    }

    @Test(dataProvider = "readers", groups = {"smoke", "regression"})
    public void getCoffees_withValidToken_returnsAllCoffees(String caseName, String token) {
        CoffeeStubs.getCoffees(wireMockServer);

        Response response = coffeeApi.getCoffees(token, null);

        assertEquals(response.statusCode(), 200, "Status mismatch for: " + caseName);

        Coffee[] coffees = response.as(Coffee[].class);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(coffees.length, 8, "Number of coffees mismatch for: " + caseName);
        softAssert.assertEquals(coffees[0].getName(), "Espresso", "First coffee mismatch for: " + caseName);
        softAssert.assertAll();
    }

    // ---------- Schema ----------

    @Test(groups = {"regression"})
    public void getCoffees_matchesSchema() {
        CoffeeStubs.getCoffees(wireMockServer);

        Response response = coffeeApi.getCoffees(Tokens.CUSTOMER, null);

        assertEquals(response.statusCode(), 200, "Status mismatch");
        response.then().assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/coffee-list-schema.json"));
    }

    // ---------- Filter by type ----------

    @DataProvider(name = "typeFilters")
    public Object[][] typeFilters() {
        return new Object[][]{
                {"ESPRESSO_BASED", 2},
                {"WHITE", 3},
                {"BLACK", 2},
                {"COLD", 1},
                {"DECAF", 0}
        };
    }

    @Test(dataProvider = "typeFilters", groups = {"regression"})
    public void getCoffees_withTypeFilter_returnsOnlyThatType(String type, int expectedCount) {
        CoffeeStubs.getCoffees(wireMockServer);

        Response response = coffeeApi.getCoffees(Tokens.CUSTOMER, type);

        assertEquals(response.statusCode(), 200, "Status mismatch for type: " + type);

        Coffee[] coffees = response.as(Coffee[].class);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(coffees.length, expectedCount, "Count mismatch for type: " + type);
        for (Coffee coffee : coffees) {
            softAssert.assertEquals(coffee.getType(), type, "Wrong type for coffee: " + coffee.getName());
        }
        softAssert.assertAll();
    }

    // ---------- Invalid type ----------

    @DataProvider(name = "invalidTypes")
    public Object[][] invalidTypes() {
        return new Object[][]{
                {"TEA"},
                {"white"}
        };
    }

    @Test(dataProvider = "invalidTypes", groups = {"regression"})
    public void getCoffees_withInvalidType_returns400(String type) {
        CoffeeStubs.getCoffees(wireMockServer);

        Response response = coffeeApi.getCoffees(Tokens.CUSTOMER, type);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 400, "Status mismatch for type: " + type);
        softAssert.assertEquals(response.jsonPath().getString("code"), "INVALID_TYPE",
                "Error code mismatch for type: " + type);
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
    public void getCoffees_withBadToken_returns401(String caseName, String token, String expectedCode) {
        Response response = coffeeApi.getCoffees(token, null);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 401, "Status mismatch for: " + caseName);
        softAssert.assertEquals(response.jsonPath().getString("code"), expectedCode,
                "Error code mismatch for: " + caseName);
        softAssert.assertAll();
    }
}