package com.codecart.coffee.mocks;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

/**
 * Stubs for the auth endpoints.
 */
public final class RegisterStubs {

    private static final String REGISTER_URL = "/auth/register";

    private RegisterStubs() {
    }

    public static void register(WireMockServer server) {
        // 1: role is sent -> 400 ROLE_NOT_ALLOWED
        server.stubFor(post(urlEqualTo(REGISTER_URL))
                .atPriority(1)
                .withRequestBody(matchingJsonPath("$.role"))
                .willReturn(StubSupport.errorResponse(400, "ROLE_NOT_ALLOWED", "Role must not be sent")));

        // 2: email already used -> 409 EMAIL_EXISTS
        server.stubFor(post(urlEqualTo(REGISTER_URL))
                .atPriority(2)
                .withRequestBody(matchingJsonPath("$.email",
                        matching("(admin@coffee\\.com|customer@test\\.com|sara@test\\.com)")))
                .willReturn(StubSupport.errorResponse(409, "EMAIL_EXISTS", "Email already exists")));

        // 3: valid body -> 201, echo name and email
        server.stubFor(post(urlEqualTo(REGISTER_URL))
                .atPriority(3)
                .withRequestBody(matchingJsonPath("$.name", matching(".{1,50}")))
                .withRequestBody(matchingJsonPath("$.email", matching("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")))
                .withRequestBody(matchingJsonPath("$.password", matching("(?=.*[A-Za-z])(?=.*\\d).{8,20}")))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":10,"
                                + "\"name\":\"{{jsonPath request.body '$.name'}}\","
                                + "\"email\":\"{{jsonPath request.body '$.email'}}\","
                                + "\"role\":\"CUSTOMER\"}")
                        .withTransformers("response-template")));

        // 4: everything else on this URL -> 400 VALIDATION_ERROR
        server.stubFor(post(urlEqualTo(REGISTER_URL))
                .atPriority(4)
                .willReturn(StubSupport.errorResponse(400, "VALIDATION_ERROR", "Validation failed")));
    }
}