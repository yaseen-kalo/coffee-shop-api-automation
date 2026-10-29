package com.codecart.coffee.tests.failures;

import com.codecart.coffee.base.BaseTest;
import com.codecart.coffee.clients.coffees.CoffeeApi;
import com.codecart.coffee.clients.orders.OrderApi;
import com.codecart.coffee.data.CoffeeFactory;
import com.codecart.coffee.data.OrderFactory;
import com.codecart.coffee.mocks.CoffeeStubs;
import com.codecart.coffee.mocks.StubSupport;
import com.codecart.coffee.utils.Tokens;
import com.github.tomakehurst.wiremock.http.Fault;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

/**
 * Tests what happens when the server misbehaves: errors, slow answers, broken connections.
 * Each stub here is used by one test only, so it lives inside the test.
 */
public class FailureScenariosTests extends BaseTest {

    private static final String CUSTOMER_AUTH = "Bearer " + Tokens.CUSTOMER;

    private final CoffeeApi coffeeApi = new CoffeeApi();
    private final OrderApi orderApi = new OrderApi();

    private RestAssuredConfig originalConfig;

    /** Some tests change the timeout. We save the config before and restore it after. */
    @BeforeMethod
    public void saveConfig() {
        originalConfig = RestAssured.config;
    }

    @AfterMethod(alwaysRun = true)
    public void restoreConfig() {
        RestAssured.config = originalConfig;
    }

    // ---------- 500 server error ----------

    @Test(groups = {"regression"})
    public void getCoffees_whenServerFails_returns500() {
        stubServerError(get(urlEqualTo("/coffees")));

        Response response = coffeeApi.getCoffees(Tokens.CUSTOMER, null);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 500, "Status mismatch");
        softAssert.assertEquals(response.jsonPath().getString("code"), "SERVER_ERROR", "Error code mismatch");
        softAssert.assertAll();
    }

    @Test(groups = {"regression"})
    public void placeOrder_whenServerFails_returns500() {
        stubServerError(post(urlEqualTo("/orders")));

        Response response = orderApi.placeOrder(Tokens.CUSTOMER, OrderFactory.validOrder());

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 500, "Status mismatch");
        softAssert.assertEquals(response.jsonPath().getString("code"), "SERVER_ERROR", "Error code mismatch");
        softAssert.assertAll();
    }

    /**
     * Checks that a 500 error has the same shape as every other error: status, code, message.
     */
    @Test(groups = {"regression"})
    public void serverError_matchesErrorSchema() {
        stubServerError(get(urlEqualTo("/coffees")));

        Response response = coffeeApi.getCoffees(Tokens.CUSTOMER, null);

        assertEquals(response.statusCode(), 500, "Status mismatch");
        response.then().assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/error-schema.json"));
    }

    // ---------- Slow response and timeout ----------

    @Test(groups = {"regression"})
    public void getCoffees_whenServerIsSlow_timesOut() {
        stubSlowCoffees(3000);
        setTimeout(1000);

        assertThrows(Exception.class, () -> coffeeApi.getCoffees(Tokens.CUSTOMER, null));
    }

    @Test(groups = {"regression"})
    public void getCoffees_whenServerIsSlowButInTime_returns200() {
        stubSlowCoffees(300);
        setTimeout(2000);

        Response response = coffeeApi.getCoffees(Tokens.CUSTOMER, null);

        assertEquals(response.statusCode(), 200, "Status mismatch");
    }

    // ---------- Broken connection ----------

    @DataProvider(name = "faults")
    public Object[][] faults() {
        return new Object[][]{
                {"connection reset by peer", Fault.CONNECTION_RESET_BY_PEER},
                {"empty response", Fault.EMPTY_RESPONSE},
                {"random data then close", Fault.RANDOM_DATA_THEN_CLOSE}
        };
    }

    @Test(dataProvider = "faults", groups = {"regression"})
    public void getCoffees_whenConnectionBreaks_throwsException(String caseName, Fault fault) {
        wireMockServer.stubFor(get(urlEqualTo("/coffees"))
                .atPriority(1)
                .withHeader("Authorization", equalTo(CUSTOMER_AUTH))
                .willReturn(aResponse().withFault(fault)));

        assertThrows(Exception.class, () -> coffeeApi.getCoffees(Tokens.CUSTOMER, null));
    }

    // ---------- verify: did the mock really receive the request? ----------

    @Test(groups = {"regression"})
    public void getCoffees_sendsOneRequestWithBearerToken() {
        CoffeeStubs.getCoffees(wireMockServer);

        coffeeApi.getCoffees(Tokens.CUSTOMER, null);

        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/coffees"))
                .withHeader("Authorization", equalTo(CUSTOMER_AUTH)));
    }

    /**
     * Checks the Gson setup: a new coffee must not send "id": null.
     */
    @Test(groups = {"regression"})
    public void createCoffee_doesNotSendId() {
        CoffeeStubs.createCoffee(wireMockServer);

        coffeeApi.createCoffee(Tokens.ADMIN, CoffeeFactory.validCoffee());

        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/coffees"))
                .withRequestBody(notMatching(".*\"id\".*")));
    }

    @Test(groups = {"regression"})
    public void getCoffees_sendsNoWriteRequests() {
        CoffeeStubs.getCoffees(wireMockServer);

        coffeeApi.getCoffees(Tokens.CUSTOMER, null);

        wireMockServer.verify(0, postRequestedFor(urlEqualTo("/coffees")));
        wireMockServer.verify(0, deleteRequestedFor(urlPathMatching("/coffees/.*")));
    }

    // ---------- Helpers ----------

    private void stubServerError(com.github.tomakehurst.wiremock.client.MappingBuilder request) {
        wireMockServer.stubFor(request
                .atPriority(1)
                .withHeader("Authorization", equalTo(CUSTOMER_AUTH))
                .willReturn(StubSupport.errorResponse(500, "SERVER_ERROR", "Internal server error")));
    }

    private void stubSlowCoffees(int delayMs) {
        wireMockServer.stubFor(get(urlEqualTo("/coffees"))
                .atPriority(1)
                .withHeader("Authorization", equalTo(CUSTOMER_AUTH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")
                        .withFixedDelay(delayMs)));
    }

    /** Keeps the Gson setting, because it starts from the current config. */
    private void setTimeout(int milliseconds) {
        RestAssured.config = RestAssured.config().httpClient(HttpClientConfig.httpClientConfig()
                .setParam("http.connection.timeout", milliseconds)
                .setParam("http.socket.timeout", milliseconds));
    }
}