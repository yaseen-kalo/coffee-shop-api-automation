package com.codecart.coffee.mocks;

import com.codecart.coffee.utils.Tokens;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

/**
 * Stubs for the coffee endpoints.
 * Read: both roles. Write: admin only, customer gets 403.
 * Bad or missing tokens are handled by the default stubs.
 */
public final class CoffeeStubs {

    private static final String COFFEES_URL = "/coffees";
    private static final String VALID_AUTH = "Bearer (admin-token|customer-token)";
    private static final String ADMIN_AUTH = "Bearer " + Tokens.ADMIN;
    private static final String CUSTOMER_AUTH = "Bearer " + Tokens.CUSTOMER;
    private static final String TYPE_REGEX = "BLACK|WHITE|ESPRESSO_BASED|COLD|DECAF";
    private static final String EXISTING_NAMES =
            "Espresso|Macchiato|Cappuccino|Latte|Flat White|Americano|Black Coffee|Iced Coffee";
    private static final String EXISTING_ID = COFFEES_URL + "/[1-8]";
    private static final String ANY_POSITIVE_ID = COFFEES_URL + "/[1-9][0-9]{0,8}";
    private static final String[] TYPES = {"BLACK", "WHITE", "ESPRESSO_BASED", "COLD", "DECAF"};

    // id, name, type, price, stock
    private static final String[][] COFFEES = {
            {"1", "Espresso", "ESPRESSO_BASED", "3.00", "50"},
            {"2", "Macchiato", "ESPRESSO_BASED", "3.50", "40"},
            {"3", "Cappuccino", "WHITE", "4.50", "40"},
            {"4", "Latte", "WHITE", "4.80", "30"},
            {"5", "Flat White", "WHITE", "4.60", "30"},
            {"6", "Americano", "BLACK", "3.20", "60"},
            {"7", "Black Coffee", "BLACK", "2.50", "100"},
            {"8", "Iced Coffee", "COLD", "4.00", "25"}
    };

    private CoffeeStubs() {
    }

    // ---------- Read ----------

    public static void getCoffees(WireMockServer server) {
        // 1: no filter -> all coffees
        server.stubFor(get(urlEqualTo(COFFEES_URL))
                .atPriority(1)
                .withHeader("Authorization", matching(VALID_AUTH))
                .willReturn(jsonOk(listJson(null))));

        // 2: valid type -> only that type (DECAF gives [])
        for (String type : TYPES) {
            server.stubFor(get(urlPathEqualTo(COFFEES_URL))
                    .atPriority(2)
                    .withHeader("Authorization", matching(VALID_AUTH))
                    .withQueryParam("type", equalTo(type))
                    .willReturn(jsonOk(listJson(type))));
        }

        // 3: any other type value -> 400 INVALID_TYPE
        server.stubFor(get(urlPathEqualTo(COFFEES_URL))
                .atPriority(3)
                .withHeader("Authorization", matching(VALID_AUTH))
                .withQueryParam("type", notMatching(TYPE_REGEX))
                .willReturn(StubSupport.errorResponse(400, "INVALID_TYPE", "Invalid coffee type")));
    }

    public static void getCoffeeById(WireMockServer server) {
        // 1: the 8 starter coffees -> 200
        for (String[] coffee : COFFEES) {
            server.stubFor(get(urlEqualTo(COFFEES_URL + "/" + coffee[0]))
                    .atPriority(1)
                    .withHeader("Authorization", matching(VALID_AUTH))
                    .willReturn(jsonOk(coffeeJson(coffee))));
        }

        // 2: a normal positive id (up to 9 digits) that does not exist -> 404
        server.stubFor(get(urlPathMatching(ANY_POSITIVE_ID))
                .atPriority(2)
                .withHeader("Authorization", matching(VALID_AUTH))
                .willReturn(StubSupport.errorResponse(404, "COFFEE_NOT_FOUND", "Coffee not found")));

        // 3: everything else (0, -1, abc, 1.5, 99999999999) -> 400 INVALID_ID
        server.stubFor(get(urlPathMatching(COFFEES_URL + "/.*"))
                .atPriority(3)
                .withHeader("Authorization", matching(VALID_AUTH))
                .willReturn(StubSupport.errorResponse(400, "INVALID_ID", "Invalid id")));
    }

    // ---------- Write (admin only) ----------

    public static void createCoffee(WireMockServer server) {
        // 1: admin, name already exists -> 409 COFFEE_EXISTS
        server.stubFor(post(urlEqualTo(COFFEES_URL))
                .atPriority(1)
                .withHeader("Authorization", equalTo(ADMIN_AUTH))
                .withRequestBody(matchingJsonPath("$.name", matching(EXISTING_NAMES)))
                .willReturn(StubSupport.errorResponse(409, "COFFEE_EXISTS", "Coffee already exists")));

        // 2: admin, valid body -> 201, new id 9, echo the body
        server.stubFor(withValidCoffeeBody(post(urlEqualTo(COFFEES_URL)))
                .atPriority(2)
                .withHeader("Authorization", equalTo(ADMIN_AUTH))
                .willReturn(echoResponse(201, "9")));

        // 3: admin, any other body -> 400 VALIDATION_ERROR
        server.stubFor(post(urlEqualTo(COFFEES_URL))
                .atPriority(3)
                .withHeader("Authorization", equalTo(ADMIN_AUTH))
                .willReturn(StubSupport.errorResponse(400, "VALIDATION_ERROR", "Validation failed")));

        // 4: customer -> 403 FORBIDDEN
        server.stubFor(post(urlEqualTo(COFFEES_URL))
                .atPriority(4)
                .withHeader("Authorization", equalTo(CUSTOMER_AUTH))
                .willReturn(forbidden()));
    }

    public static void updateCoffee(WireMockServer server) {
        // 1: admin, existing id, valid body -> 200, echo the body with the id from the URL
        server.stubFor(withValidCoffeeBody(put(urlPathMatching(EXISTING_ID)))
                .atPriority(1)
                .withHeader("Authorization", equalTo(ADMIN_AUTH))
                .willReturn(echoResponse(200, "{{request.pathSegments.[1]}}")));

        // 2: admin, existing id, bad body -> 400 VALIDATION_ERROR
        server.stubFor(put(urlPathMatching(EXISTING_ID))
                .atPriority(2)
                .withHeader("Authorization", equalTo(ADMIN_AUTH))
                .willReturn(StubSupport.errorResponse(400, "VALIDATION_ERROR", "Validation failed")));

        // 3: admin, id does not exist -> 404
        server.stubFor(put(urlPathMatching(ANY_POSITIVE_ID))
                .atPriority(3)
                .withHeader("Authorization", equalTo(ADMIN_AUTH))
                .willReturn(StubSupport.errorResponse(404, "COFFEE_NOT_FOUND", "Coffee not found")));

        // 4: admin, bad id -> 400 INVALID_ID
        server.stubFor(put(urlPathMatching(COFFEES_URL + "/.*"))
                .atPriority(4)
                .withHeader("Authorization", equalTo(ADMIN_AUTH))
                .willReturn(StubSupport.errorResponse(400, "INVALID_ID", "Invalid id")));

        // 5: customer -> 403
        server.stubFor(put(urlPathMatching(COFFEES_URL + "/.*"))
                .atPriority(5)
                .withHeader("Authorization", equalTo(CUSTOMER_AUTH))
                .willReturn(forbidden()));
    }

    public static void updateStock(WireMockServer server) {
        // 1: admin, existing id, stock is a whole number 0 or more -> 200, full coffee with new stock
        for (String[] coffee : COFFEES) {
            server.stubFor(patch(urlEqualTo(COFFEES_URL + "/" + coffee[0] + "/stock"))
                    .atPriority(1)
                    .withHeader("Authorization", equalTo(ADMIN_AUTH))
                    .withRequestBody(matchingJsonPath("$.stock", matching("\\d+")))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(coffeeJsonWithStock(coffee, "{{jsonPath request.body '$.stock'}}"))
                            .withTransformers("response-template")));
        }

        // 2: admin, existing id, bad stock (negative or text) -> 400
        server.stubFor(patch(urlPathMatching(EXISTING_ID + "/stock"))
                .atPriority(2)
                .withHeader("Authorization", equalTo(ADMIN_AUTH))
                .willReturn(StubSupport.errorResponse(400, "VALIDATION_ERROR", "Validation failed")));

        // 3: admin, id does not exist -> 404
        server.stubFor(patch(urlPathMatching(ANY_POSITIVE_ID + "/stock"))
                .atPriority(3)
                .withHeader("Authorization", equalTo(ADMIN_AUTH))
                .willReturn(StubSupport.errorResponse(404, "COFFEE_NOT_FOUND", "Coffee not found")));

        // 4: customer -> 403
        server.stubFor(patch(urlPathMatching(COFFEES_URL + "/[^/]+/stock"))
                .atPriority(4)
                .withHeader("Authorization", equalTo(CUSTOMER_AUTH))
                .willReturn(forbidden()));
    }

    public static void deleteCoffee(WireMockServer server) {
        // 1: admin, existing id -> 204, no body
        server.stubFor(delete(urlPathMatching(EXISTING_ID))
                .atPriority(1)
                .withHeader("Authorization", equalTo(ADMIN_AUTH))
                .willReturn(aResponse().withStatus(204)));

        // 2: admin, id does not exist -> 404
        server.stubFor(delete(urlPathMatching(ANY_POSITIVE_ID))
                .atPriority(2)
                .withHeader("Authorization", equalTo(ADMIN_AUTH))
                .willReturn(StubSupport.errorResponse(404, "COFFEE_NOT_FOUND", "Coffee not found")));

        // 3: customer -> 403
        server.stubFor(delete(urlPathMatching(COFFEES_URL + "/[^/]+"))
                .atPriority(3)
                .withHeader("Authorization", equalTo(CUSTOMER_AUTH))
                .willReturn(forbidden()));
    }

    // ---------- Helpers ----------

    /** Adds the rules for a valid coffee body: name, type, price and stock. */
    private static MappingBuilder withValidCoffeeBody(MappingBuilder builder) {
        return builder
                .withRequestBody(matchingJsonPath("$.name", matching(".{1,50}")))
                .withRequestBody(matchingJsonPath("$.type", matching(TYPE_REGEX)))
                .withRequestBody(matchingJsonPath("$[?(@.price > 0 && @.price <= 100)]"))
                .withRequestBody(matchingJsonPath("$.stock", matching("\\d+")));
    }

    private static ResponseDefinitionBuilder forbidden() {
        return StubSupport.errorResponse(403, "FORBIDDEN", "Admin only");
    }

    private static ResponseDefinitionBuilder echoResponse(int status, String idValue) {
        return aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":" + idValue
                        + ",\"name\":\"{{jsonPath request.body '$.name'}}\""
                        + ",\"type\":\"{{jsonPath request.body '$.type'}}\""
                        + ",\"price\":{{jsonPath request.body '$.price'}}"
                        + ",\"stock\":{{jsonPath request.body '$.stock'}}}")
                .withTransformers("response-template");
    }

    private static ResponseDefinitionBuilder jsonOk(String body) {
        return aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
    }

    private static String listJson(String type) {
        List<String> items = new ArrayList<>();
        for (String[] coffee : COFFEES) {
            if (type == null || coffee[2].equals(type)) {
                items.add(coffeeJson(coffee));
            }
        }
        return "[" + String.join(",", items) + "]";
    }

    private static String coffeeJson(String[] c) {
        return coffeeJsonWithStock(c, c[4]);
    }

    private static String coffeeJsonWithStock(String[] c, String stock) {
        return "{\"id\":" + c[0] + ",\"name\":\"" + c[1] + "\",\"type\":\"" + c[2]
                + "\",\"price\":" + c[3] + ",\"stock\":" + stock + "}";
    }
}