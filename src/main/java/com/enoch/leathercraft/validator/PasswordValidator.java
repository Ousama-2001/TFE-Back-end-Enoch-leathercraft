package com.enoch.leathercraft.validator;

import java.util.regex.Pattern;

public final class PasswordValidator {

    // Au moins 8 caractères, 1 minuscule, 1 majuscule, 1 chiffre
    // 👉 pas d'obligation de caractère spécial
    private static final Pattern PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");

    private PasswordValidator() {}

    public static boolean isStrongPassword(String raw) {
        if (raw == null) return false;
        return PATTERN.matcher(raw).matches();
    }
}
