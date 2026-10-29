package com.codecart.coffee.data;

import com.codecart.coffee.utils.Tokens;
import org.testng.annotations.DataProvider;

public class AuthDataProviders {

    private AuthDataProviders() {
    }

    @DataProvider(name = "badTokens")
    public static Object[][] badTokens() {
        return new Object[][]{
                {"no token", null, "INVALID_TOKEN"},
                {"unknown token", "bad-token", "INVALID_TOKEN"},
                {"expired token", Tokens.EXPIRED, "TOKEN_EXPIRED"}
        };
    }
}
