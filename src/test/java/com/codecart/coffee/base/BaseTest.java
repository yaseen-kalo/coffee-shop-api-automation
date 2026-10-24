package com.codecart.coffee.base;

import com.codecart.coffee.mocks.StubSupport;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.mapper.ObjectMapperType;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeMethod;

/**
 * Parent class for all tests.
 * Starts one WireMock server before the suite and stops it after.
 * Points Rest Assured to the mock server and forces Gson.
 * Resets all stubs before each test, so tests do not affect each other.
 */

public class BaseTest {

    /** Shared by all test classes, so stubs can be added from any test. */
    protected static WireMockServer wireMockServer;

    @BeforeSuite
    public void startServer() {

        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = wireMockServer.port();
        WireMock.configureFor("localhost", wireMockServer.port());

        RestAssured.config = RestAssuredConfig.config()
                .objectMapperConfig(ObjectMapperConfig
                        .objectMapperConfig()
                        .defaultObjectMapperType(ObjectMapperType.GSON));
    }

    @BeforeMethod
    public void resetStubs() {
        wireMockServer.resetAll();
        StubSupport.registerDefaults(wireMockServer);
    }



    @AfterSuite
    public void stopServer() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }
}