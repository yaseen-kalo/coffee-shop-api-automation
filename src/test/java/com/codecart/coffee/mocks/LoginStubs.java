package com.codecart.coffee.mocks;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

/**
 * Stubs for POST /auth/login.
 * Known users get a token. Wrong credentials get 401. Missing fields get 400.
 */
public final class LoginStubs {

    private static final String LOGIN_URL = "/auth/login";

    private LoginStubs() {
    }

    public static void login(WireMockServer server) {
        // 1 to 3: seed users with the right password -> 200 and a token
        loginSuccess(server, 1, "customer@test.com", "Pass@1234", "customer-token", "CUSTOMER");
        loginSuccess(server, 2, "admin@coffee.com", "Admin@123", "admin-token", "ADMIN");
        loginSuccess(server, 3, "sara@test.com", "Pass@1234", "customer-token", "CUSTOMER");

        // 4: both fields are sent, but they do not match a user -> 401 INVALID_CREDENTIALS
        server.stubFor(post(urlEqualTo(LOGIN_URL))
                .atPriority(4)
                .withRequestBody(matchingJsonPath("$.email", matching(".+")))
                .withRequestBody(matchingJsonPath("$.password", matching(".+")))
                .willReturn(StubSupport.errorResponse(401, "INVALID_CREDENTIALS", "Invalid email or password")));

        // 5: everything else (a field is missing) -> 400 VALIDATION_ERROR
        server.stubFor(post(urlEqualTo(LOGIN_URL))
                .atPriority(5)
                .willReturn(StubSupport.errorResponse(400, "VALIDATION_ERROR", "Validation failed")));
    }

    private static void loginSuccess(WireMockServer server, int priority, String email,
                                     String password, String token, String role) {
        server.stubFor(post(urlEqualTo(LOGIN_URL))
                .atPriority(priority)
                .withRequestBody(matchingJsonPath("$.email", equalTo(email)))
                .withRequestBody(matchingJsonPath("$.password", equalTo(password)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"token\":\"" + token + "\",\"role\":\"" + role + "\"}")));
    }
}