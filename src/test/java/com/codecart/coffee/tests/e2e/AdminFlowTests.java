package com.codecart.coffee.tests.e2e;

import com.codecart.coffee.clients.coffees.CoffeeApi;
import com.codecart.coffee.data.CoffeeFactory;
import com.codecart.coffee.data.LoginFactory;
import com.codecart.coffee.mocks.FlowStubs;
import com.codecart.coffee.mocks.LoginStubs;
import com.codecart.coffee.models.coffees.Coffee;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class AdminFlowTests extends E2EBase {

    private final CoffeeApi coffeeApi = new CoffeeApi();

    /**
     * Admin flow: login, create a coffee, read it, update it, change the stock,
     * delete it, and read it again (404). Each step uses the result of the step before.
     */
    @Test(groups = {"e2e", "regression"})
    public void adminFlow_createReadUpdateChangeStockDelete() {
        LoginStubs.login(wireMockServer);
        FlowStubs.adminCoffeeFlow(wireMockServer);

        String token = loginAndGetToken(LoginFactory.validAdminLogin());

        // Step 1: create
        Coffee newCoffee = CoffeeFactory.flowCoffee();
        Response created = coffeeApi.createCoffee(token, newCoffee);
        assertEquals(created.statusCode(), 201, "Step 1 (create): status mismatch");
        Coffee createdCoffee = created.as(Coffee.class);
        assertNotNull(createdCoffee.getId(), "Step 1 (create): id should be set by the server");
        String id = String.valueOf(createdCoffee.getId());

        // Step 2: read it
        Response fetched = coffeeApi.getCoffeeById(token, id);
        assertEquals(fetched.statusCode(), 200, "Step 2 (read): status mismatch");
        assertEquals(fetched.as(Coffee.class).getName(), newCoffee.getName(), "Step 2 (read): name mismatch");

        // Step 3: update it
        Coffee updateRequest = CoffeeFactory.flowCoffeeUpdated();
        Response updated = coffeeApi.updateCoffee(token, id, updateRequest);
        assertEquals(updated.statusCode(), 200, "Step 3 (update): status mismatch");
        Coffee updatedCoffee = updated.as(Coffee.class);
        assertEquals(updatedCoffee.getName(), updateRequest.getName(), "Step 3 (update): name mismatch");
        assertEquals(updatedCoffee.getPrice(), updateRequest.getPrice(), 0.001, "Step 3 (update): price mismatch");

        // Step 4: change the stock
        Response stockChanged = coffeeApi.updateStock(token, id, 25);
        assertEquals(stockChanged.statusCode(), 200, "Step 4 (stock): status mismatch");
        assertEquals(stockChanged.as(Coffee.class).getStock(), Integer.valueOf(25), "Step 4 (stock): stock mismatch");

        // Step 5: delete it
        Response deleted = coffeeApi.deleteCoffee(token, id);
        assertEquals(deleted.statusCode(), 204, "Step 5 (delete): status mismatch");

        // Step 6: read it again, it must be gone
        Response gone = coffeeApi.getCoffeeById(token, id);
        assertEquals(gone.statusCode(), 404, "Step 6 (read after delete): status mismatch");
        assertEquals(gone.jsonPath().getString("code"), "COFFEE_NOT_FOUND",
                "Step 6 (read after delete): error code mismatch");
    }
}