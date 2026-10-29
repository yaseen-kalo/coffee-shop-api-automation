package com.codecart.coffee.mocks;

import com.codecart.coffee.utils.Tokens;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

/**
 * Stubs for the order endpoints.
 * Customer: place, list, cancel. Admin: change status. Both: read one order.
 * Bad or missing tokens are handled by the default stubs.
 */
public final class OrderStubs {

    private static final String ORDERS_URL = "/orders";
    private static final String ADMIN_AUTH = "Bearer " + Tokens.ADMIN;
    private static final String CUSTOMER_AUTH = "Bearer " + Tokens.CUSTOMER;
    private static final String VALID_AUTH = "Bearer (admin-token|customer-token)";
    private static final String STATUS_REGEX = "PLACED|PREPARING|READY|COMPLETED";
    private static final String ANY_POSITIVE_ID = ORDERS_URL + "/[1-9][0-9]{0,8}";
    private static final String SEED_ID = ORDERS_URL + "/[1-3]";

    private static final String SINGLE_BODY = "{\"items\":[{\"coffeeId\":3,\"quantity\":2}]}";
    private static final String MULTI_BODY =
            "{\"items\":[{\"coffeeId\":3,\"quantity\":2},{\"coffeeId\":6,\"quantity\":1}]}";

    // id, status, items, total
    private static final String[][] ORDERS = {
            {"1", "PLACED", "[{\"coffeeId\":3,\"quantity\":2}]", "9.00"},
            {"2", "PREPARING", "[{\"coffeeId\":6,\"quantity\":1}]", "3.20"},
            {"3", "COMPLETED", "[{\"coffeeId\":1,\"quantity\":1}]", "3.00"}
    };

    private OrderStubs() {
    }

    // ---------- POST /orders (customer) ----------

    public static void placeOrder(WireMockServer server) {
        // 1: admin -> 403
        server.stubFor(post(urlEqualTo(ORDERS_URL))
                .atPriority(1)
                .withHeader("Authorization", equalTo(ADMIN_AUTH))
                .willReturn(forbidden("Customers only")));

        // 2: a quantity below 1 or above 10 -> 400
        server.stubFor(customerPost(2)
                .withRequestBody(matchingJsonPath("$.items[?(@.quantity < 1 || @.quantity > 10)]"))
                .willReturn(validationError()));

        // 2: more than 5 lines -> 400
        server.stubFor(customerPost(2)
                .withRequestBody(matchingJsonPath("$.items[5]"))
                .willReturn(validationError()));

        // 3: a coffee id that does not exist -> 404
        server.stubFor(customerPost(3)
                .withRequestBody(matchingJsonPath("$.items[?(@.coffeeId < 1 || @.coffeeId > 8)]"))
                .willReturn(StubSupport.errorResponse(404, "COFFEE_NOT_FOUND", "Coffee not found")));

        // 4: the one-line example -> 201, total 9.00
        server.stubFor(customerPost(4)
                .withRequestBody(equalToJson(SINGLE_BODY))
                .willReturn(json(201, orderJson("10", "PLACED", "[{\"coffeeId\":3,\"quantity\":2}]", "9.00"))));

        // 4: the two-line example from the contract -> 201, total 12.20
        server.stubFor(customerPost(4)
                .withRequestBody(equalToJson(MULTI_BODY))
                .willReturn(json(201, orderJson("10", "PLACED",
                        "[{\"coffeeId\":3,\"quantity\":2},{\"coffeeId\":6,\"quantity\":1}]", "12.20"))));

        // 5: any other order with at least one line -> 201 (fixed answer)
        server.stubFor(customerPost(5)
                .withRequestBody(matchingJsonPath("$.items[0]"))
                .willReturn(json(201, orderJson("10", "PLACED", "[{\"coffeeId\":3,\"quantity\":1}]", "4.50"))));

        // 6: everything else (no items, empty items) -> 400
        server.stubFor(customerPost(6)
                .willReturn(validationError()));
    }

    /** Add this one after placeOrder: coffee 8 is out of stock. */
    public static void placeOrderOutOfStock(WireMockServer server) {
        server.stubFor(customerPost(1)
                .withRequestBody(matchingJsonPath("$.items[?(@.coffeeId == 8)]"))
                .willReturn(StubSupport.errorResponse(409, "OUT_OF_STOCK", "Not enough stock")));
    }

    // ---------- GET /orders (customer) ----------

    public static void getOrders(WireMockServer server) {
        // 1: customer -> 200, the three seed orders
        server.stubFor(get(urlEqualTo(ORDERS_URL))
                .atPriority(1)
                .withHeader("Authorization", equalTo(CUSTOMER_AUTH))
                .willReturn(json(200, allOrdersJson())));

        // 2: admin -> 403
        server.stubFor(get(urlEqualTo(ORDERS_URL))
                .atPriority(2)
                .withHeader("Authorization", equalTo(ADMIN_AUTH))
                .willReturn(forbidden("Customers only")));
    }

    public static void getOrdersEmpty(WireMockServer server) {
        server.stubFor(get(urlEqualTo(ORDERS_URL))
                .atPriority(1)
                .withHeader("Authorization", equalTo(CUSTOMER_AUTH))
                .willReturn(json(200, "[]")));
    }

    // ---------- GET /orders/{id} (both roles) ----------

    public static void getOrderById(WireMockServer server) {
        // 1: the three seed orders -> 200
        for (String[] order : ORDERS) {
            server.stubFor(get(urlEqualTo(ORDERS_URL + "/" + order[0]))
                    .atPriority(1)
                    .withHeader("Authorization", matching(VALID_AUTH))
                    .willReturn(json(200, orderJson(order))));
        }

        // 2: a normal positive id that does not exist -> 404
        server.stubFor(get(urlPathMatching(ANY_POSITIVE_ID))
                .atPriority(2)
                .withHeader("Authorization", matching(VALID_AUTH))
                .willReturn(StubSupport.errorResponse(404, "ORDER_NOT_FOUND", "Order not found")));

        // 3: everything else (0, -1, abc, 1.5, 99999999999) -> 400
        server.stubFor(get(urlPathMatching(ORDERS_URL + "/.*"))
                .atPriority(3)
                .withHeader("Authorization", matching(VALID_AUTH))
                .willReturn(StubSupport.errorResponse(400, "INVALID_ID", "Invalid id")));
    }

    // ---------- PATCH /orders/{id}/cancel (customer) ----------

    public static void cancelOrder(WireMockServer server) {
        // 1: order 1 is PLACED -> 200 CANCELLED
        server.stubFor(patch(urlEqualTo(ORDERS_URL + "/1/cancel"))
                .atPriority(1)
                .withHeader("Authorization", equalTo(CUSTOMER_AUTH))
                .willReturn(json(200, orderJson("1", "CANCELLED", "[{\"coffeeId\":3,\"quantity\":2}]", "9.00"))));

        // 2: orders 2 and 3 are already further -> 409
        server.stubFor(patch(urlPathMatching(ORDERS_URL + "/[23]/cancel"))
                .atPriority(2)
                .withHeader("Authorization", equalTo(CUSTOMER_AUTH))
                .willReturn(StubSupport.errorResponse(409, "CANNOT_CANCEL", "Order cannot be cancelled")));

        // 3: other ids -> 404
        server.stubFor(patch(urlPathMatching(ANY_POSITIVE_ID + "/cancel"))
                .atPriority(3)
                .withHeader("Authorization", equalTo(CUSTOMER_AUTH))
                .willReturn(StubSupport.errorResponse(404, "ORDER_NOT_FOUND", "Order not found")));

        // 4: admin -> 403
        server.stubFor(patch(urlPathMatching(ORDERS_URL + "/[^/]+/cancel"))
                .atPriority(4)
                .withHeader("Authorization", equalTo(ADMIN_AUTH))
                .willReturn(forbidden("Customers only")));
    }

    // ---------- PATCH /orders/{id}/status (admin) ----------

    public static void updateStatus(WireMockServer server) {
        // 1: the two allowed steps in the mock: order 1 PLACED -> PREPARING, order 2 PREPARING -> READY
        server.stubFor(patch(urlEqualTo(ORDERS_URL + "/1/status"))
                .atPriority(1)
                .withHeader("Authorization", equalTo(ADMIN_AUTH))
                .withRequestBody(matchingJsonPath("$.status", equalTo("PREPARING")))
                .willReturn(json(200, orderJson("1", "PREPARING", "[{\"coffeeId\":3,\"quantity\":2}]", "9.00"))));
        server.stubFor(patch(urlEqualTo(ORDERS_URL + "/2/status"))
                .atPriority(1)
                .withHeader("Authorization", equalTo(ADMIN_AUTH))
                .withRequestBody(matchingJsonPath("$.status", equalTo("READY")))
                .willReturn(json(200, orderJson("2", "READY", "[{\"coffeeId\":6,\"quantity\":1}]", "3.20"))));

        // 1: customer -> 403
        server.stubFor(patch(urlPathMatching(ORDERS_URL + "/[^/]+/status"))
                .atPriority(1)
                .withHeader("Authorization", equalTo(CUSTOMER_AUTH))
                .willReturn(forbidden("Admin only")));

        // 2: admin, status is not one of the four values (FLYING, empty, lower case) -> 400
        server.stubFor(patch(urlPathMatching(ORDERS_URL + "/[^/]+/status"))
                .atPriority(2)
                .withHeader("Authorization", equalTo(ADMIN_AUTH))
                .withRequestBody(matchingJsonPath("$.status", notMatching(STATUS_REGEX)))
                .willReturn(validationError()));

        // 3: admin, existing order, a step that is not allowed -> 409
        server.stubFor(patch(urlPathMatching(SEED_ID + "/status"))
                .atPriority(3)
                .withHeader("Authorization", equalTo(ADMIN_AUTH))
                .willReturn(StubSupport.errorResponse(409, "INVALID_STATUS_TRANSITION",
                        "Status change not allowed")));

        // 4: admin, order does not exist -> 404
        server.stubFor(patch(urlPathMatching(ANY_POSITIVE_ID + "/status"))
                .atPriority(4)
                .withHeader("Authorization", equalTo(ADMIN_AUTH))
                .willReturn(StubSupport.errorResponse(404, "ORDER_NOT_FOUND", "Order not found")));

        // 5: admin, anything else (missing status, bad id) -> 400
        server.stubFor(patch(urlPathMatching(ORDERS_URL + "/[^/]+/status"))
                .atPriority(5)
                .withHeader("Authorization", equalTo(ADMIN_AUTH))
                .willReturn(validationError()));
    }

    // ---------- Helpers ----------

    private static MappingBuilder customerPost(int priority) {
        return post(urlEqualTo(ORDERS_URL))
                .atPriority(priority)
                .withHeader("Authorization", equalTo(CUSTOMER_AUTH));
    }

    private static ResponseDefinitionBuilder forbidden(String message) {
        return StubSupport.errorResponse(403, "FORBIDDEN", message);
    }

    private static ResponseDefinitionBuilder validationError() {
        return StubSupport.errorResponse(400, "VALIDATION_ERROR", "Validation failed");
    }

    private static ResponseDefinitionBuilder json(int status, String body) {
        return aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
    }

    private static String allOrdersJson() {
        List<String> items = new ArrayList<>();
        for (String[] order : ORDERS) {
            items.add(orderJson(order));
        }
        return "[" + String.join(",", items) + "]";
    }

    private static String orderJson(String[] o) {
        return orderJson(o[0], o[1], o[2], o[3]);
    }

    private static String orderJson(String id, String status, String items, String total) {
        return "{\"id\":" + id + ",\"status\":\"" + status + "\",\"items\":" + items
                + ",\"total\":" + total + "}";
    }
}