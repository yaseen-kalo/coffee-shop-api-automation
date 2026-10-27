package com.codecart.coffee.tests.auth;

import com.codecart.coffee.base.BaseTest;
import com.codecart.coffee.clients.auth.RegisterApi;
import com.codecart.coffee.data.RegisterFactory;
import com.codecart.coffee.mocks.AuthStubs;
import com.codecart.coffee.models.auth.register.RegisterRequest;
import com.codecart.coffee.models.auth.register.RegisterResponse;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.testng.Assert.assertEquals;

public class RegisterTests extends BaseTest {

    private final RegisterApi registerApi = new RegisterApi();

    // ---------- Valid cases ----------

    @DataProvider(name = "validRegisterRequests")
    public Object[][] validRegisterRequests() {
        return new Object[][]{
                {"random valid user", RegisterFactory.validRegisterPayload()},
                {"name with 1 character", RegisterFactory.withName("A")},
                {"name with 50 characters", RegisterFactory.withName("A".repeat(50))},
                {"password with 8 characters", RegisterFactory.withPassword("Pass1234")},
                {"password with 20 characters", RegisterFactory.withPassword("A".repeat(19) + "1")}
        };
    }

    @Test(dataProvider = "validRegisterRequests", groups = {"smoke", "regression"})
    public void register_withValidData_returns201(String caseName, RegisterRequest request) {
        AuthStubs.register(wireMockServer);

        Response response = registerApi.register(request);

        assertEquals(response.statusCode(), 201, "Status code mismatch for: " + caseName);

        RegisterResponse actual = response.as(RegisterResponse.class);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertNotNull(actual.getId(), "Id should not be null");
        softAssert.assertEquals(actual.getName(), request.getName(), "Name mismatch");
        softAssert.assertEquals(actual.getEmail(), request.getEmail(), "Email mismatch");
        softAssert.assertEquals(actual.getRole(), "CUSTOMER", "Role mismatch");
        softAssert.assertNull(response.jsonPath().getString("password"), "Password must not be returned");
        softAssert.assertAll();
    }

    // ---------- Password boundaries ----------

    @DataProvider(name = "passwordCases")
    public Object[][] passwordCases() {
        return new Object[][]{
                {"7 characters", "Pass123", 400},
                {"8 characters", "Pass1234", 201},
                {"20 characters", "A".repeat(19) + "1", 201},
                {"21 characters", "A".repeat(20) + "1", 400},
                {"letters only", "Password", 400},
                {"numbers only", "12345678", 400}
        };
    }

    @Test(dataProvider = "passwordCases", groups = {"regression"})
    public void register_withPasswordCase_returnsExpectedStatus(String caseName, String password,
                                                                int expectedStatus) {
        AuthStubs.register(wireMockServer);

        Response response = registerApi.register(RegisterFactory.withPassword(password));

        assertEquals(response.statusCode(), expectedStatus, "Status mismatch for: " + caseName);
    }

    // ---------- Invalid cases ----------

    @DataProvider(name = "invalidRegisterRequests")
    public Object[][] invalidRegisterRequests() {
        return new Object[][]{
                {"name is missing", RegisterFactory.withoutName(), 400, "VALIDATION_ERROR"},
                {"email is missing", RegisterFactory.withoutEmail(), 400, "VALIDATION_ERROR"},
                {"password is missing", RegisterFactory.withoutPassword(), 400, "VALIDATION_ERROR"},
                {"name is empty", RegisterFactory.withName(""), 400, "VALIDATION_ERROR"},
                {"name has 51 characters", RegisterFactory.withName("A".repeat(51)), 400, "VALIDATION_ERROR"},
                {"email has no @", RegisterFactory.withEmail("plainaddress"), 400, "VALIDATION_ERROR"},
                {"email has no domain dot", RegisterFactory.withEmail("a@b"), 400, "VALIDATION_ERROR"},
                {"role CUSTOMER is sent", RegisterFactory.withRole("CUSTOMER"), 400, "ROLE_NOT_ALLOWED"},
                {"role ADMIN is sent", RegisterFactory.withRole("ADMIN"), 400, "ROLE_NOT_ALLOWED"}
        };
    }

    @Test(dataProvider = "invalidRegisterRequests", groups = {"regression"})
    public void register_withInvalidData_returnsError(String caseName, RegisterRequest request,
                                                      int expectedStatus, String expectedCode) {
        AuthStubs.register(wireMockServer);

        Response response = registerApi.register(request);

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), expectedStatus, "Status mismatch for: " + caseName);
        softAssert.assertEquals(response.jsonPath().getString("code"), expectedCode,
                "Error code mismatch for: " + caseName);
        softAssert.assertAll();
    }

    // ---------- Duplicate email ----------

    @DataProvider(name = "existingEmails")
    public Object[][] existingEmails() {
        return new Object[][]{
                {"admin@coffee.com"},
                {"customer@test.com"},
                {"sara@test.com"}
        };
    }

    @Test(dataProvider = "existingEmails", groups = {"regression"})
    public void register_withExistingEmail_returns409(String email) {
        AuthStubs.register(wireMockServer);

        Response response = registerApi.register(RegisterFactory.withEmail(email));

        SoftAssert softAssert = new SoftAssert();
        softAssert.assertEquals(response.statusCode(), 409, "Status mismatch for: " + email);
        softAssert.assertEquals(response.jsonPath().getString("code"), "EMAIL_EXISTS", "Error code mismatch");
        softAssert.assertAll();
    }


    // ---------- Schema ----------

    @Test(groups = {"regression"})
    public void register_withValidData_matchesSchema() {
        AuthStubs.register(wireMockServer);

        Response response = registerApi.register(RegisterFactory.validRegisterPayload());

        assertEquals(response.statusCode(), 201, "Status code mismatch");
        response.then().assertThat()
                .body(matchesJsonSchemaInClasspath("schemas/register-response-schema.json"));
    }
}