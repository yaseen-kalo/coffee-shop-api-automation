package com.codecart.coffee.mocks;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

/**
 * Stubs for the read endpoints: GET /coffees and GET /coffees/{id}.
 * Both admin and customer can read. Bad tokens are handled by the default stubs.
 */
public final class CoffeeStubs {

    private static final String COFFEES_URL = "/coffees";
    private static final String VALID_AUTH = "Bearer (admin-token|customer-token)";
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
                .withQueryParam("type", notMatching("BLACK|WHITE|ESPRESSO_BASED|COLD|DECAF"))
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
        server.stubFor(get(urlPathMatching(COFFEES_URL + "/[1-9][0-9]{0,8}"))
                .atPriority(2)
                .withHeader("Authorization", matching(VALID_AUTH))
                .willReturn(StubSupport.errorResponse(404, "COFFEE_NOT_FOUND", "Coffee not found")));

        // 3: everything else (0, -1, abc, 1.5, 99999999999) -> 400 INVALID_ID
        server.stubFor(get(urlPathMatching(COFFEES_URL + "/.*"))
                .atPriority(3)
                .withHeader("Authorization", matching(VALID_AUTH))
                .willReturn(StubSupport.errorResponse(400, "INVALID_ID", "Invalid id")));
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
        return "{\"id\":" + c[0] + ",\"name\":\"" + c[1] + "\",\"type\":\"" + c[2]
                + "\",\"price\":" + c[3] + ",\"stock\":" + c[4] + "}";
    }
}