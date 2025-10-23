package com.enoch.leathercraft.auth.service;

import com.enoch.leathercraft.auth.dto.AuthRequest;
import com.enoch.leathercraft.auth.dto.AuthResponse;
import com.enoch.leathercraft.auth.dto.RegisterRequest;
import com.enoch.leathercraft.auth.domain.Role;
import com.enoch.leathercraft.auth.domain.User;
import com.enoch.leathercraft.auth.repo.UserRepository;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;

    public AuthService(UserRepository repo, PasswordEncoder encoder, AuthenticationManager authManager, JwtService jwt) {
        this.repo = repo; this.encoder = encoder; this.authManager = authManager; this.jwt = jwt;
    }

    public AuthResponse register(RegisterRequest req){
        if (repo.existsByUsername(req.username()))
            throw new IllegalArgumentException("Username already exists");
        Role role = "ADMIN".equalsIgnoreCase(req.role()) ? Role.ADMIN : Role.USER;
        User u = repo.save(User.builder()
                .username(req.username())
                .password(encoder.encode(req.password()))
                .role(role)
                .build());
        return new AuthResponse(jwt.generate(u.getUsername(), u.getRole().name()));
    }

    public AuthResponse login(AuthRequest req){
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        User u = repo.findByUsername(req.username()).orElseThrow();
        return new AuthResponse(jwt.generate(u.getUsername(), u.getRole().name()));
    }
}
