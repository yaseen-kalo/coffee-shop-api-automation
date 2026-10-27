package com.codecart.coffee.tests.auth;

import com.codecart.coffee.base.BaseTest;
import com.codecart.coffee.clients.auth.LoginApi;
import com.codecart.coffee.data.LoginFactory;
import com.codecart.coffee.mocks.LoginStubs;
import com.codecart.coffee.models.auth.login.LoginRequest;
import com.codecart.coffee.models.auth.login.LoginResponse;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.testng.Assert.assertEquals;

public class LoginTests extends BaseTest {

    private final LoginApi loginApi = new LoginApi();

    // ---------- Valid login ----------

    @DataProvider(name = "validUsers")
    public Object[][] validUsers() {
        return new Object[][]{
                {"customer", "customer@test.com", "Pass@1234", "customer-token", "CUSTOMER"},
                {"admin", "admin@coffee.com", "Admin@123", "admin-token", "ADMIN"},
                {"sara", "sara@test.com", "Pass@1234", "customer-token", "CUSTOMER"}
        };
    }

    @Test(dataProvider = "validUsers", groups = {"smoke", "regression"})
    public void login_withValidUser_returns200AndToken(String caseName, String email, String password,
                                                       String expectedToken, String expectedRole) {
        LoginStubs.login(wireMockServer);

        Response response = loginApi.login(LoginFactory.withCredentials(email, password));

        assertEquals(response.statusCode(), 200, "Status mismatch for: " + caseName);

        LoginResponse actual = response.as(LoginResponse.class);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(actual.getToken(), expectedToken, "Token mismatch for: " + caseName);
        softAssert.assertEquals(actual.getRole(), expectedRole, "Role mismatch for: " + caseName);
        softAssert.assertAll();
    }

    // ---------- Wrong credentials ----------

    @DataProvider(name = "wrongCredentials")
    public Object[][] wrongCredentials() {
        return new Object[][]{
                {"wrong password", LoginFactory.withPassword("WrongPass1")},
                {"wrong email", LoginFactory.withEmail("nobody@test.com")},
                {"unknown user", LoginFactory.withCredentials("new.user@test.com", "Pass@1234")},
                {"admin email with customer password",
                        LoginFactory.withCredentials("admin@coffee.com", "Pass@1234")}
        };
    }

    @Test(dataProvider = "wrongCredentials", groups = {"regression"})
    public void login_withWrongCredentials_returns401(String caseName, LoginRequest request) {
        LoginStubs.login(wireMockServer);

        Response response = loginApi.login(request);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 401, "Status mismatch for: " + caseName);
        softAssert.assertEquals(response.jsonPath().getString("code"), "INVALID_CREDENTIALS",
                "Error code mismatch for: " + caseName);
        softAssert.assertAll();
    }

    // ---------- Missing or empty fields ----------

    @DataProvider(name = "invalidLoginRequests")
    public Object[][] invalidLoginRequests() {
        return new Object[][]{
                {"email is missing", LoginFactory.withoutEmail()},
                {"password is missing", LoginFactory.withoutPassword()},
                {"email is empty", LoginFactory.withEmail("")}
        };
    }

    @Test(dataProvider = "invalidLoginRequests", groups = {"regression"})
    public void login_withMissingField_returns400(String caseName, LoginRequest request) {
        LoginStubs.login(wireMockServer);

        Response response = loginApi.login(request);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 400, "Status mismatch for: " + caseName);
        softAssert.assertEquals(response.jsonPath().getString("code"), "VALIDATION_ERROR",
                "Error code mismatch for: " + caseName);
        softAssert.assertAll();
    }

    // ---------- Schema ----------

    @Test(groups = {"regression"})
    public void login_withValidUser_matchesSchema() {
        LoginStubs.login(wireMockServer);

        Response response = loginApi.login(LoginFactory.validLogin());

        assertEquals(response.statusCode(), 200, "Status mismatch");
        response.then().assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/login-response-schema.json"));
    }
}