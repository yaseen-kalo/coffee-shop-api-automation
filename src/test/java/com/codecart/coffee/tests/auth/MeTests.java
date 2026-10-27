package com.codecart.coffee.tests.auth;

import com.codecart.coffee.base.BaseTest;
import com.codecart.coffee.clients.auth.MeApi;
import com.codecart.coffee.mocks.MeStubs;
import com.codecart.coffee.models.auth.me.MeResponse;
import com.codecart.coffee.utils.Tokens;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.testng.Assert.assertEquals;

public class MeTests extends BaseTest {

    private final MeApi meApi = new MeApi();

    // ---------- Valid tokens ----------

    @DataProvider(name = "validTokens")
    public Object[][] validTokens() {
        return new Object[][]{
                {"admin", Tokens.ADMIN, "admin@coffee.com", "ADMIN"},
                {"customer", Tokens.CUSTOMER, "customer@test.com", "CUSTOMER"}
        };
    }

    @Test(dataProvider = "validTokens", groups = {"smoke", "regression"})
    public void me_withValidToken_returns200(String caseName, String token,
                                             String expectedEmail, String expectedRole) {
        MeStubs.me(wireMockServer);

        Response response = meApi.me(token);

        assertEquals(response.statusCode(), 200, "Status mismatch for: " + caseName);

        MeResponse actual = response.as(MeResponse.class);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertNotNull(actual.getId(), "Id should not be null");
        softAssert.assertEquals(actual.getEmail(), expectedEmail, "Email mismatch for: " + caseName);
        softAssert.assertEquals(actual.getRole(), expectedRole, "Role mismatch for: " + caseName);
        softAssert.assertNull(response.jsonPath().getString("password"), "Password must not be returned");
        softAssert.assertAll();
    }

    // ---------- Bad tokens ----------

    @DataProvider(name = "badTokens")
    public Object[][] badTokens() {
        return new Object[][]{
                {"no token", null, 401, "INVALID_TOKEN"},
                {"unknown token", "bad-token", 401, "INVALID_TOKEN"},
                {"expired token", Tokens.EXPIRED, 401, "TOKEN_EXPIRED"}
        };
    }

    @Test(dataProvider = "badTokens", groups = {"regression"})
    public void me_withBadToken_returns401(String caseName, String token,
                                           int expectedStatus, String expectedCode) {
        Response response = meApi.me(token);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), expectedStatus, "Status mismatch for: " + caseName);
        softAssert.assertEquals(response.jsonPath().getString("code"), expectedCode,
                "Error code mismatch for: " + caseName);
        softAssert.assertAll();
    }

    // ---------- Schema ----------

    @Test(groups = {"regression"})
    public void me_withValidToken_matchesSchema() {
        MeStubs.me(wireMockServer);

        Response response = meApi.me(Tokens.CUSTOMER);

        assertEquals(response.statusCode(), 200, "Status mismatch");
        response.then().assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/me-response-schema.json"));
    }
}