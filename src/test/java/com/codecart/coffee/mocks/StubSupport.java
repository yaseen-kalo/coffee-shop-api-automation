package com.codecart.coffee.mocks;

import com.codecart.coffee.utils.Tokens;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;

/**
 * Helpers for the mock server.
 * Registers the default stubs that answer when no real stub matches.
 * Stubs responses for expired token, invalid token, and valid token but no stub.
 */
public final class StubSupport {

    private StubSupport() {
    }

    public static void registerDefaults(WireMockServer server) {
        // 8: expired token -> 401 TOKEN_EXPIRED
        server.stubFor(any(anyUrl())
                .atPriority(8)
                .withHeader("Authorization", equalTo("Bearer " + Tokens.EXPIRED))
                .willReturn(errorResponse(401, "TOKEN_EXPIRED", "Token expired")));

        // 9: valid token, but no real stub for this URL -> 404 ROUTE_NOT_FOUND
        server.stubFor(any(anyUrl())
                .atPriority(9)
                .withHeader("Authorization",
                        matching("Bearer (" + Tokens.ADMIN + "|" + Tokens.CUSTOMER + ")"))
                .willReturn(errorResponse(404, "ROUTE_NOT_FOUND", "Route not found")));

        // 10: anything else (no token, bad token) -> 401 INVALID_TOKEN
        server.stubFor(any(anyUrl())
                .atPriority(10)
                .willReturn(errorResponse(401, "INVALID_TOKEN", "Invalid token")));
    }

    public static ResponseDefinitionBuilder errorResponse(int status, String code, String message) {
        return aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\":\"error\",\"code\":\"" + code
                        + "\",\"message\":\"" + message + "\"}");
    }
}