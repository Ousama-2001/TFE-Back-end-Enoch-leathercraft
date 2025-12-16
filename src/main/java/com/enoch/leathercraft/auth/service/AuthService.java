// src/main/java/com/enoch/leathercraft/auth/service/AuthService.java
package com.enoch.leathercraft.auth.service;

import com.enoch.leathercraft.auth.domain.Role;
import com.enoch.leathercraft.auth.domain.User;
import com.enoch.leathercraft.auth.dto.AuthRequest;
import com.enoch.leathercraft.auth.dto.AuthResponse;
import com.enoch.leathercraft.auth.dto.RegisterRequest;
import com.enoch.leathercraft.auth.repo.UserRepository;
import com.enoch.leathercraft.services.MailService;
import com.enoch.leathercraft.validator.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    // ✅ Mail welcome
    private final MailService mailService;

    public AuthResponse register(RegisterRequest request) {

        // ✅ validations "required"
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("EMAIL_REQUIRED");
        }
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("USERNAME_REQUIRED");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("PASSWORD_REQUIRED");
        }

        // ✅ normalisation (anti-doublons)
        String email = request.getEmail().trim().toLowerCase();
        String username = request.getUsername().trim().toLowerCase(); // 🔥 IMPORTANT

        request.setEmail(email);
        request.setUsername(username);

        // ✅ mot de passe fort
        if (!PasswordValidator.isStrongPassword(request.getPassword())) {
            throw new IllegalArgumentException("WEAK_PASSWORD");
        }

        // ✅ unicité CASE-INSENSITIVE
        if (users.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException("EMAIL_ALREADY_USED");
        }
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new IllegalStateException("USERNAME_ALREADY_USED");
        }

        // ✅ création utilisateur
        User u = new User();
        u.setEmail(email);
        u.setUsername(username); // ✅ stocké en minuscule
        u.setPasswordHash(encoder.encode(request.getPassword()));
        u.setRole(Role.CUSTOMER);
        u.setFirstName(request.getFirstName() == null ? null : request.getFirstName().trim());
        u.setLastName(request.getLastName() == null ? null : request.getLastName().trim());

        users.save(u);

        // ✅ mail de bienvenue (ne bloque pas l'inscription si le mail échoue)
        try {
            mailService.sendWelcomeEmail(u.getEmail(), u.getFirstName());
        } catch (Exception ignored) {}

        String token = jwt.generateToken(u.getEmail(), u.getRole().name());
        return AuthResponse.builder()
                .token(token)
                .role(u.getRole().name())
                .build();
    }

    public AuthResponse login(AuthRequest request) {

        String identifier = request.getIdentifier();
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("IDENTIFIER_REQUIRED");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("PASSWORD_REQUIRED");
        }

        String id = identifier.trim();

        // ✅ login insensible à la casse pour email + username
        User u = users.findByEmailIgnoreCase(id)
                .or(() -> users.findByUsernameIgnoreCase(id))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (u.isDeleted()) {
            throw new IllegalStateException("ACCOUNT_DELETED");
        }

        if (!encoder.matches(request.getPassword(), u.getPasswordHash())) {
            throw new BadCredentialsException("BAD_CREDENTIALS");
        }

        String token = jwt.generateToken(u.getEmail(), u.getRole().name());
        return AuthResponse.builder()
                .token(token)
                .role(u.getRole().name())
                .build();
    }
}
