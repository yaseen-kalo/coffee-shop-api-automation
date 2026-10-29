package com.codecart.coffee.mocks;

import com.codecart.coffee.utils.Tokens;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.ScenarioMappingBuilder;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

/**
 * Stubs with memory, for the e2e flows. They use WireMock Scenarios:
 * a stub answers only in a certain state, and it can move the scenario to a new state.
 * BaseTest resets all scenarios before each test, so flows do not leak into each other.
 */
public final class FlowStubs {

    private static final String ADMIN_AUTH = "Bearer " + Tokens.ADMIN;
    private static final String CUSTOMER_AUTH = "Bearer " + Tokens.CUSTOMER;
    private static final String VALID_AUTH = "Bearer (" + Tokens.ADMIN + "|" + Tokens.CUSTOMER + ")";

    private static final String ADMIN_COFFEE_FLOW = "admin-coffee-flow";
    private static final String ORDER_FLOW = "order-flow";
    private static final String REGISTRATION_FLOW = "registration-flow";
    private static final String STOCK_FLOW = "stock-flow";

    private static final String[] ORDER_STATUSES =
            {"PLACED", "PREPARING", "READY", "COMPLETED", "CANCELLED"};
    private static final String[][] STATUS_STEPS =
            {{"PLACED", "PREPARING"}, {"PREPARING", "READY"}, {"READY", "COMPLETED"}};

    private FlowStubs() {
    }

    // ---------- Flow 1: admin manages one coffee (id 9) ----------
    // Started -> CREATED -> UPDATED -> STOCK_CHANGED -> DELETED

    public static void adminCoffeeFlow(WireMockServer server) {
        String created = coffeeJson("9", "Flow Coffee", "WHITE", "4.25", "10");
        String updated = coffeeJson("9", "Flow Coffee Updated", "ESPRESSO_BASED", "5.50", "10");
        String restocked = coffeeJson("9", "Flow Coffee Updated", "ESPRESSO_BASED", "5.50", "25");

        step(server, post(urlEqualTo("/coffees")).withHeader("Authorization", equalTo(ADMIN_AUTH)),
                ADMIN_COFFEE_FLOW, Scenario.STARTED, "CREATED", json(201, created));

        step(server, get(urlEqualTo("/coffees/9")).withHeader("Authorization", matching(VALID_AUTH)),
                ADMIN_COFFEE_FLOW, "CREATED", null, json(200, created));

        step(server, put(urlEqualTo("/coffees/9")).withHeader("Authorization", equalTo(ADMIN_AUTH)),
                ADMIN_COFFEE_FLOW, "CREATED", "UPDATED", json(200, updated));

        step(server, patch(urlEqualTo("/coffees/9/stock")).withHeader("Authorization", equalTo(ADMIN_AUTH)),
                ADMIN_COFFEE_FLOW, "UPDATED", "STOCK_CHANGED", json(200, restocked));

        step(server, delete(urlEqualTo("/coffees/9")).withHeader("Authorization", equalTo(ADMIN_AUTH)),
                ADMIN_COFFEE_FLOW, "STOCK_CHANGED", "DELETED", aResponse().withStatus(204));

        step(server, get(urlEqualTo("/coffees/9")).withHeader("Authorization", matching(VALID_AUTH)),
                ADMIN_COFFEE_FLOW, "DELETED", null,
                StubSupport.errorResponse(404, "COFFEE_NOT_FOUND", "Coffee not found"));
    }

    // ---------- Flows 2, 3, 4: one order (id 10) ----------
    // The state name is the order status: Started -> PLACED -> PREPARING -> READY -> COMPLETED
    // A customer cancel moves PLACED -> CANCELLED.

    public static void orderFlow(WireMockServer server) {
        // place the order
        step(server, post(urlEqualTo("/orders"))
                        .withHeader("Authorization", equalTo(CUSTOMER_AUTH))
                        .withRequestBody(matchingJsonPath("$.items[0]")),
                ORDER_FLOW, Scenario.STARTED, "PLACED", json(201, flowOrderJson("PLACED")));

        // read the order: the answer shows the current status
        for (String status : ORDER_STATUSES) {
            step(server, get(urlEqualTo("/orders/10")).withHeader("Authorization", matching(VALID_AUTH)),
                    ORDER_FLOW, status, null, json(200, flowOrderJson(status)));
        }

        // admin moves the status forward, one allowed step at a time
        for (String[] stepPair : STATUS_STEPS) {
            step(server, patch(urlEqualTo("/orders/10/status"))
                            .withHeader("Authorization", equalTo(ADMIN_AUTH))
                            .withRequestBody(matchingJsonPath("$.status", equalTo(stepPair[1]))),
                    ORDER_FLOW, stepPair[0], stepPair[1], json(200, flowOrderJson(stepPair[1])));
        }

        // customer cancels: allowed only while PLACED
        step(server, patch(urlEqualTo("/orders/10/cancel")).withHeader("Authorization", equalTo(CUSTOMER_AUTH)),
                ORDER_FLOW, "PLACED", "CANCELLED", json(200, flowOrderJson("CANCELLED")));

        for (String status : new String[]{"PREPARING", "READY", "COMPLETED", "CANCELLED"}) {
            step(server, patch(urlEqualTo("/orders/10/cancel")).withHeader("Authorization", equalTo(CUSTOMER_AUTH)),
                    ORDER_FLOW, status, null,
                    StubSupport.errorResponse(409, "CANNOT_CANCEL", "Order cannot be cancelled"));
        }
    }

    // ---------- Flow 3: register, then login works ----------
    // Started: login fails. REGISTERED: login works.

    public static void registerThenLogin(WireMockServer server, String name, String email, String password) {
        step(server, post(urlEqualTo("/auth/register"))
                        .withRequestBody(matchingJsonPath("$.email", equalTo(email))),
                REGISTRATION_FLOW, Scenario.STARTED, "REGISTERED",
                json(201, "{\"id\":10,\"name\":\"" + name + "\",\"email\":\"" + email
                        + "\",\"role\":\"CUSTOMER\"}"));

        step(server, post(urlEqualTo("/auth/login"))
                        .withRequestBody(matchingJsonPath("$.email", equalTo(email))),
                REGISTRATION_FLOW, Scenario.STARTED, null,
                StubSupport.errorResponse(401, "INVALID_CREDENTIALS", "Invalid email or password"));

        step(server, post(urlEqualTo("/auth/login"))
                        .withRequestBody(matchingJsonPath("$.email", equalTo(email)))
                        .withRequestBody(matchingJsonPath("$.password", equalTo(password))),
                REGISTRATION_FLOW, "REGISTERED", null,
                json(200, "{\"token\":\"" + Tokens.CUSTOMER + "\",\"role\":\"CUSTOMER\"}"));
    }

    // ---------- Flow 5: stock of coffee 1 ----------
    // Started -> LOW (admin sets 2) -> SOLD_OUT (customer orders 2) -> RESTOCKED (admin sets 10)

    public static void stockFlow(WireMockServer server) {
        String espressoLow = coffeeJson("1", "Espresso", "ESPRESSO_BASED", "3.00", "2");
        String espressoRestocked = coffeeJson("1", "Espresso", "ESPRESSO_BASED", "3.00", "10");

        step(server, patch(urlEqualTo("/coffees/1/stock"))
                        .withHeader("Authorization", equalTo(ADMIN_AUTH))
                        .withRequestBody(matchingJsonPath("$.stock", equalTo("2"))),
                STOCK_FLOW, Scenario.STARTED, "LOW", json(200, espressoLow));

        step(server, post(urlEqualTo("/orders"))
                        .withHeader("Authorization", equalTo(CUSTOMER_AUTH))
                        .withRequestBody(matchingJsonPath("$.items[0]")),
                STOCK_FLOW, "LOW", "SOLD_OUT",
                json(201, orderJson("10", "PLACED", "[{\"coffeeId\":1,\"quantity\":2}]", "6.00")));

        step(server, post(urlEqualTo("/orders"))
                        .withHeader("Authorization", equalTo(CUSTOMER_AUTH))
                        .withRequestBody(matchingJsonPath("$.items[0]")),
                STOCK_FLOW, "SOLD_OUT", null,
                StubSupport.errorResponse(409, "OUT_OF_STOCK", "Not enough stock"));

        step(server, patch(urlEqualTo("/coffees/1/stock"))
                        .withHeader("Authorization", equalTo(ADMIN_AUTH))
                        .withRequestBody(matchingJsonPath("$.stock", equalTo("10"))),
                STOCK_FLOW, "SOLD_OUT", "RESTOCKED", json(200, espressoRestocked));

        step(server, post(urlEqualTo("/orders"))
                        .withHeader("Authorization", equalTo(CUSTOMER_AUTH))
                        .withRequestBody(matchingJsonPath("$.items[0]")),
                STOCK_FLOW, "RESTOCKED", null,
                json(201, orderJson("11", "PLACED", "[{\"coffeeId\":1,\"quantity\":1}]", "3.00")));
    }

    // ---------- Helpers ----------

    /** One scenario step: answer only in state "from", then move to state "to" (null = stay). */
    private static void step(WireMockServer server, MappingBuilder request, String scenario,
                             String fromState, String toState, ResponseDefinitionBuilder response) {
        ScenarioMappingBuilder builder = request
                .atPriority(1)
                .inScenario(scenario)
                .whenScenarioStateIs(fromState);

        if (toState != null) {
            builder = builder.willSetStateTo(toState);
        }

        server.stubFor(builder.willReturn(response));
    }

    private static ResponseDefinitionBuilder json(int status, String body) {
        return aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
    }

    private static String coffeeJson(String id, String name, String type, String price, String stock) {
        return "{\"id\":" + id + ",\"name\":\"" + name + "\",\"type\":\"" + type
                + "\",\"price\":" + price + ",\"stock\":" + stock + "}";
    }

    private static String flowOrderJson(String status) {
        return orderJson("10", status, "[{\"coffeeId\":3,\"quantity\":2}]", "9.00");
    }

    private static String orderJson(String id, String status, String items, String total) {
        return "{\"id\":" + id + ",\"status\":\"" + status + "\",\"items\":" + items
                + ",\"total\":" + total + "}";
    }
}