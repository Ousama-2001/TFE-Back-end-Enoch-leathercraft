package com.enoch.leathercraft.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 👉 choisis ton mot de passe ici :
        String rawPassword = "admin";

        String hash = encoder.encode(rawPassword);
        System.out.println("Hash BCrypt = " + hash);
    }
}
