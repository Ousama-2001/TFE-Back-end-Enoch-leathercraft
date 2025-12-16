// src/main/java/com/enoch/leathercraft/auth/controller/AuthController.java
package com.enoch.leathercraft.auth.controller;

import com.enoch.leathercraft.auth.domain.PasswordResetToken;
import com.enoch.leathercraft.auth.domain.User;
import com.enoch.leathercraft.auth.dto.AuthRequest;
import com.enoch.leathercraft.auth.dto.AuthResponse;
import com.enoch.leathercraft.auth.dto.RegisterRequest;
import com.enoch.leathercraft.auth.repo.UserRepository;
import com.enoch.leathercraft.auth.service.AuthService;
import com.enoch.leathercraft.services.MailService;
import com.enoch.leathercraft.auth.repo.PasswordResetTokenRepository;
import com.enoch.leathercraft.superadmin.service.SuperAdminRequestService;
import com.enoch.leathercraft.validator.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    // Super admin (demandes de réactivation)
    private final SuperAdminRequestService superAdminRequestService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    public record MeResponse(String email, String role) {}

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_CUSTOMER");

        return ResponseEntity.ok(new MeResponse(email, role));
    }

    // =============== MOT DE PASSE OUBLIÉ : DEMANDE ===============
    @PostMapping("/password-reset-request")
    public ResponseEntity<?> requestPasswordReset(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body("Email requis.");
        }

        String normalized = email.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(normalized);

        if (userOpt.isEmpty()) {
            // Ne pas révéler si l'email existe ou pas
            return ResponseEntity.ok().build();
        }

        User user = userOpt.get();
        PasswordResetToken token = PasswordResetToken.forUser(user, 30);
        passwordResetTokenRepository.save(token);

        String resetLink = "http://localhost:4200/reset-password?token=" + token.getToken();
        mailService.sendPasswordResetLink(user.getEmail(), resetLink);

        return ResponseEntity.ok().build();
    }

    // =============== MOT DE PASSE OUBLIÉ : RESET ===============
    @PostMapping("/password-reset")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> payload) {
        String tokenValue = payload.get("token");
        String newPassword = payload.get("newPassword");

        if (tokenValue == null || newPassword == null) {
            return ResponseEntity.badRequest().body("Token et mot de passe sont requis.");
        }

        var tokenOpt = passwordResetTokenRepository.findByToken(tokenValue);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Lien de réinitialisation invalide.");
        }

        PasswordResetToken token = tokenOpt.get();
        if (token.isExpired()) {
            return ResponseEntity.badRequest().body("Lien de réinitialisation expiré.");
        }

        User user = token.getUser();

        if (!PasswordValidator.isStrongPassword(newPassword)) {
            return ResponseEntity.badRequest().body("Mot de passe trop faible.");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            return ResponseEntity.badRequest().body("Le nouveau mot de passe doit être différent de l'ancien.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordResetTokenRepository.delete(token);

        mailService.sendPasswordChangedEmail(user.getEmail());

        return ResponseEntity.ok().build();
    }

    // =============== DEMANDE DE RÉACTIVATION ===============
    @PostMapping("/reactivation-request")
    public ResponseEntity<?> requestReactivation(@RequestBody Map<String, String> payload) {

        String email = payload.get("email");
        String message = payload.get("message");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body("EMAIL_REQUIRED");
        }

        boolean created = superAdminRequestService.createReactivationRequest(email, message);

        if (!created) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("REQUEST_ALREADY_EXISTS");
        }

        return ResponseEntity.ok("REQUEST_SENT");
    }

    // ✅ Disponibilité pseudo (CASE-INSENSITIVE)
    @GetMapping("/availability/username")
    public ResponseEntity<Map<String, Boolean>> isUsernameAvailable(@RequestParam String username) {
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("available", false));
        }

        String normalized = username.trim().toLowerCase();
        boolean available = !userRepository.existsByUsernameIgnoreCase(normalized);

        return ResponseEntity.ok(Map.of("available", available));
    }

    // ✅ Disponibilité email (CASE-INSENSITIVE)
    @GetMapping("/availability/email")
    public ResponseEntity<Map<String, Boolean>> isEmailAvailable(@RequestParam String email) {
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("available", false));
        }

        String normalized = email.trim().toLowerCase();
        boolean available = !userRepository.existsByEmailIgnoreCase(normalized);

        return ResponseEntity.ok(Map.of("available", available));
    }
}
