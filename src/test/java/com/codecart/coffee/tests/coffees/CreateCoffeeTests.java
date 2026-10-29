package com.codecart.coffee.tests.coffees;

import com.codecart.coffee.base.BaseTest;
import com.codecart.coffee.clients.coffees.CoffeeApi;
import com.codecart.coffee.data.AuthDataProviders;
import com.codecart.coffee.data.CoffeeFactory;
import com.codecart.coffee.mocks.CoffeeStubs;
import com.codecart.coffee.models.coffees.Coffee;
import com.codecart.coffee.utils.Tokens;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.testng.Assert.assertEquals;

public class CreateCoffeeTests extends BaseTest {

    private final CoffeeApi coffeeApi = new CoffeeApi();

    // ---------- Valid cases ----------

    @DataProvider(name = "validCoffees")
    public Object[][] validCoffees() {
        return new Object[][]{
                {"random valid coffee", CoffeeFactory.validCoffee()},
                {"name with 1 character", CoffeeFactory.withName("A")},
                {"name with 50 characters", CoffeeFactory.withName("A".repeat(50))},
                {"price 0.01", CoffeeFactory.withPrice(0.01)},
                {"price 100", CoffeeFactory.withPrice(100.0)},
                {"stock 0", CoffeeFactory.withStock(0)},
                {"stock 1", CoffeeFactory.withStock(1)}
        };
    }

    @Test(dataProvider = "validCoffees", groups = {"smoke", "regression"})
    public void createCoffee_withValidData_returns201(String caseName, Coffee request) {
        CoffeeStubs.createCoffee(wireMockServer);

        Response response = coffeeApi.createCoffee(Tokens.ADMIN, request);

        assertEquals(response.statusCode(), 201, "Status mismatch for: " + caseName);

        Coffee actual = response.as(Coffee.class);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertNotNull(actual.getId(), "Id should be set by the server");
        softAssert.assertEquals(actual.getName(), request.getName(), "Name mismatch for: " + caseName);
        softAssert.assertEquals(actual.getType(), request.getType(), "Type mismatch for: " + caseName);
        softAssert.assertEquals(actual.getPrice(), request.getPrice(), 0.001, "Price mismatch for: " + caseName);
        softAssert.assertEquals(actual.getStock(), request.getStock(), "Stock mismatch for: " + caseName);
        softAssert.assertAll();
    }

    // ---------- Invalid cases ----------

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
    public void createCoffee_withInvalidData_returns400(String caseName, Coffee request) {
        CoffeeStubs.createCoffee(wireMockServer);

        Response response = coffeeApi.createCoffee(Tokens.ADMIN, request);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 400, "Status mismatch for: " + caseName);
        softAssert.assertEquals(response.jsonPath().getString("code"), "VALIDATION_ERROR",
                "Error code mismatch for: " + caseName);
        softAssert.assertAll();
    }

    // ---------- Duplicate name ----------

    @DataProvider(name = "existingNames")
    public Object[][] existingNames() {
        return new Object[][]{
                {"Espresso"},
                {"Latte"}
        };
    }

    @Test(dataProvider = "existingNames", groups = {"regression"})
    public void createCoffee_withExistingName_returns409(String name) {
        CoffeeStubs.createCoffee(wireMockServer);

        Response response = coffeeApi.createCoffee(Tokens.ADMIN, CoffeeFactory.withName(name));

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 409, "Status mismatch for: " + name);
        softAssert.assertEquals(response.jsonPath().getString("code"), "COFFEE_EXISTS",
                "Error code mismatch for: " + name);
        softAssert.assertAll();
    }

    // ---------- Customer ----------

    @Test(groups = {"smoke", "regression"})
    public void createCoffee_asCustomer_returns403() {
        CoffeeStubs.createCoffee(wireMockServer);

        Response response = coffeeApi.createCoffee(Tokens.CUSTOMER, CoffeeFactory.validCoffee());

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 403, "Status mismatch");
        softAssert.assertEquals(response.jsonPath().getString("code"), "FORBIDDEN", "Error code mismatch");
        softAssert.assertAll();
    }

    // ---------- Bad tokens ----------


    @Test(dataProvider = "badTokens", dataProviderClass = AuthDataProviders.class, groups = {"regression"})
    public void createCoffee_withBadToken_returns401(String caseName, String token, String expectedCode) {
        Response response = coffeeApi.createCoffee(token, CoffeeFactory.validCoffee());

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 401, "Status mismatch for: " + caseName);
        softAssert.assertEquals(response.jsonPath().getString("code"), expectedCode,
                "Error code mismatch for: " + caseName);
        softAssert.assertAll();
    }

    // ---------- Schema ----------

    @Test(groups = {"regression"})
    public void createCoffee_withValidData_matchesSchema() {
        CoffeeStubs.createCoffee(wireMockServer);

        Response response = coffeeApi.createCoffee(Tokens.ADMIN, CoffeeFactory.validCoffee());

        assertEquals(response.statusCode(), 201, "Status mismatch");
        response.then().assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/coffee-schema.json"));
    }
}