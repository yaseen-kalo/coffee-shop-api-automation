package com.codecart.coffee.data;

import com.codecart.coffee.models.auth.register.RegisterRequest;
import net.datafaker.Faker;

public final class RegisterFactory {

    private static final Faker FAKER = new Faker();

    private RegisterFactory() {
    }

    public static RegisterRequest validRegisterPayload() {

        String name = FAKER.name().fullName();
        String email = FAKER.internet().emailAddress();
        String password = FAKER.internet().password(8, 20, true, false, true);

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName(name);
        registerRequest.setEmail(email);
        registerRequest.setPassword(password);
        return registerRequest;
    }

    public static RegisterRequest withName(String name) {
        RegisterRequest registerRequest = validRegisterPayload();
        registerRequest.setName(name);
        return registerRequest;
    }

    public static RegisterRequest withEmail(String email) {
        RegisterRequest registerRequest = validRegisterPayload();
        registerRequest.setEmail(email);
        return registerRequest;
    }

    public static RegisterRequest withPassword(String password) {
        RegisterRequest registerRequest = validRegisterPayload();
        registerRequest.setPassword(password);
        return registerRequest;
    }

    public static RegisterRequest withRole(String role) {
        RegisterRequest registerRequest = validRegisterPayload();
        registerRequest.setRole(role);
        return registerRequest;
    }

    public static RegisterRequest withoutName() {
        RegisterRequest registerRequest = validRegisterPayload();
        registerRequest.setName(null);
        return registerRequest;
    }

    public static RegisterRequest withoutEmail() {
        RegisterRequest registerRequest = validRegisterPayload();
        registerRequest.setEmail(null);
        return registerRequest;
    }

    public static RegisterRequest withoutPassword() {
        RegisterRequest registerRequest = validRegisterPayload();
        registerRequest.setPassword(null);
        return registerRequest;
    }
}
