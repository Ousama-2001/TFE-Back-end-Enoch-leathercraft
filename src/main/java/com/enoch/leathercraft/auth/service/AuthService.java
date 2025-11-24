// src/main/java/com/enoch/leathercraft/auth/service/AuthService.java
package com.enoch.leathercraft.auth.service;

import com.enoch.leathercraft.auth.domain.Role;
import com.enoch.leathercraft.auth.domain.User;
import com.enoch.leathercraft.auth.dto.AuthRequest;
import com.enoch.leathercraft.auth.dto.AuthResponse;
import com.enoch.leathercraft.auth.dto.RegisterRequest;
import com.enoch.leathercraft.auth.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    // ---------- REGISTER ----------
    public AuthResponse register(RegisterRequest request) {

        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("EMAIL_REQUIRED");
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("USERNAME_REQUIRED");
        }
        if (request.getPassword() == null || request.getPassword().length() < 4) {
            throw new IllegalArgumentException("PASSWORD_TOO_SHORT");
        }

        if (users.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("EMAIL_ALREADY_USED");
        }
        if (users.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("USERNAME_ALREADY_USED");
        }

        User u = new User();
        u.setEmail(request.getEmail());
        u.setPasswordHash(encoder.encode(request.getPassword()));
        u.setRole(Role.CUSTOMER);

        u.setFirstName(request.getFirstName());
        u.setLastName(request.getLastName());
        u.setUsername(request.getUsername());

        users.save(u);

        String token = jwt.generateToken(u.getEmail(), u.getRole().name());
        return AuthResponse.builder()
                .token(token)
                .role(u.getRole().name())
                .build();
    }

    // ---------- LOGIN ----------
    public AuthResponse login(AuthRequest request) {

        String identifier = request.getIdentifier();
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("IDENTIFIER_REQUIRED");
        }

        // On n’expose pas si l’email existe ou pas => message générique
        User u = users.findByEmail(identifier)
                .or(() -> users.findByUsername(identifier))
                .orElseThrow(() -> new IllegalArgumentException("BAD_CREDENTIALS"));

        if (!encoder.matches(request.getPassword(), u.getPasswordHash())) {
            throw new IllegalArgumentException("BAD_CREDENTIALS");
        }

        String token = jwt.generateToken(u.getEmail(), u.getRole().name());
        return AuthResponse.builder()
                .token(token)
                .role(u.getRole().name())
                .build();
    }
}
