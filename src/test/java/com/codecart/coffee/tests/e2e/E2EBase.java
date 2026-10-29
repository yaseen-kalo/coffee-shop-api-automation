package com.codecart.coffee.tests.e2e;

import com.codecart.coffee.base.BaseTest;
import com.codecart.coffee.clients.auth.LoginApi;
import com.codecart.coffee.models.auth.login.LoginRequest;
import com.codecart.coffee.models.auth.login.LoginResponse;
import io.restassured.response.Response;

import static org.testng.Assert.assertEquals;

/**
 * Parent class for the e2e flows.
 * Has the login step that every flow starts with. The test must register the login stubs first.
 */
public abstract class E2EBase extends BaseTest {

    private final LoginApi loginApi = new LoginApi();

    /** Logs in, checks that it worked, and returns the token for the next steps. */
    protected String loginAndGetToken(LoginRequest request) {
        Response response = loginApi.login(request);

        assertEquals(response.statusCode(), 200, "Login step: status mismatch");

        return response.as(LoginResponse.class).getToken();
    }
}