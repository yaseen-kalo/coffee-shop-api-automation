package com.codecart.coffee.data;

import com.codecart.coffee.models.auth.login.LoginRequest;

public final class LoginFactory {

    private static final String CUSTOMER_EMAIL = "customer@test.com";
    private static final String CUSTOMER_PASSWORD = "Pass@1234";

    private LoginFactory() {
    }

    public static LoginRequest withCredentials(String email, String password) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);
        return loginRequest;
    }

    public static LoginRequest validLogin() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(CUSTOMER_EMAIL);
        loginRequest.setPassword(CUSTOMER_PASSWORD);
        return loginRequest;
    }

    public static LoginRequest withEmail(String email) {
        LoginRequest loginRequest = validLogin();
        loginRequest.setEmail(email);
        return loginRequest;
    }

    public static LoginRequest withPassword(String password) {
        LoginRequest loginRequest = validLogin();
        loginRequest.setPassword(password);
        return loginRequest;
    }

    public static LoginRequest withoutEmail() {
        LoginRequest loginRequest = validLogin();
        loginRequest.setEmail(null);
        return loginRequest;
    }

    public static LoginRequest withoutPassword() {
        LoginRequest loginRequest = validLogin();
        loginRequest.setPassword(null);
        return loginRequest;
    }
}