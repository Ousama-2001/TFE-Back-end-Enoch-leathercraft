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

@Service @RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthResponse register(RegisterRequest req) {
        if (users.existsByEmail(req.email())) throw new RuntimeException("Email already used");
        if (req.username()!=null && !req.username().isBlank() && users.existsByUsername(req.username()))
            throw new RuntimeException("Username already used");

        var u = new User();
        u.setEmail(req.email());
        u.setFirstName(req.firstName());
        u.setLastName(req.lastName());
        u.setUsername(req.username());
        u.setRole(Role.CUSTOMER);
        u.setPasswordHash(encoder.encode(req.password()));
        users.save(u);

        return new AuthResponse(jwt.generateToken(u.getEmail(), u.getRole().name()));
    }

    public AuthResponse login(AuthRequest req) {          // 👈 LA MÉTHODE MANQUANTE
        var u = users.findByEmail(req.email())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!encoder.matches(req.password(), u.getPasswordHash()))
            throw new RuntimeException("Invalid credentials");
        return new AuthResponse(jwt.generateToken(u.getEmail(), u.getRole().name()));
    }
}
