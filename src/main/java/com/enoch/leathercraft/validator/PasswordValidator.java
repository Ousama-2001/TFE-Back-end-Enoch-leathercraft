package com.enoch.leathercraft.validator;

public class PasswordValidator {

    // Min 8, 1 maj, 1 chiffre, 1 spécial
    private static final String STRONG_REGEX =
            "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

    public static boolean isStrongPassword(String password) {
        return password != null && password.matches(STRONG_REGEX);
    }
}
