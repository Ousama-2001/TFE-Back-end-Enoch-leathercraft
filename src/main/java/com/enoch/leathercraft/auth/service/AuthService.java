// src/main/java/com/enoch/leathercraft/auth/service/AuthService.java
package com.enoch.leathercraft.auth.service;

import com.enoch.leathercraft.auth.domain.Role;
import com.enoch.leathercraft.auth.domain.User;
import com.enoch.leathercraft.auth.dto.AuthResponse;
import com.enoch.leathercraft.auth.dto.RegisterRequest;
import com.enoch.leathercraft.auth.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthResponse register(RegisterRequest req) {
        if (users.existsByUsername(req.getEmail())) {
            return new AuthResponse("Email déjà utilisé");
        }
        var u = User.builder()
                .username(req.getEmail())                // <<< évite ton 500 (NOT NULL)
                .password(encoder.encode(req.getPassword()))
                .role(Role.USER)
                .build();
        users.save(u);
        return new AuthResponse("Inscription ok");
    }
}
