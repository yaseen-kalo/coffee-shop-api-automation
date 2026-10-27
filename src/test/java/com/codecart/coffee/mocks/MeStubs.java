package com.codecart.coffee.mocks;

import com.codecart.coffee.utils.Tokens;
import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

/**
 * Stubs for GET /auth/me.
 * Admin and customer tokens get their user. Other tokens are handled by the default stubs.
 */
public final class MeStubs {

    private static final String ME_URL = "/auth/me";

    private MeStubs() {
    }

    public static void me(WireMockServer server) {
        meSuccess(server, 1, Tokens.ADMIN,
                "{\"id\":1,\"name\":\"Admin\",\"email\":\"admin@coffee.com\",\"role\":\"ADMIN\"}");
        meSuccess(server, 2, Tokens.CUSTOMER,
                "{\"id\":2,\"name\":\"Customer\",\"email\":\"customer@test.com\",\"role\":\"CUSTOMER\"}");
    }

    private static void meSuccess(WireMockServer server, int priority, String token, String body) {
        server.stubFor(get(urlEqualTo(ME_URL))
                .atPriority(priority)
                .withHeader("Authorization", equalTo("Bearer " + token))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));
    }
}