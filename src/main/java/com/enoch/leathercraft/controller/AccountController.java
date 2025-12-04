// src/main/java/com/enoch/leathercraft/controller/AccountController.java
package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.auth.domain.User;
import com.enoch.leathercraft.auth.repo.UserRepository;
import com.enoch.leathercraft.dto.ChangePasswordRequest;
import com.enoch.leathercraft.dto.ProfileResponse;
import com.enoch.leathercraft.dto.ProfileUpdateRequest;
import com.enoch.leathercraft.dto.OrderResponse;

import com.enoch.leathercraft.services.MailService;
import com.enoch.leathercraft.services.OrderService;
import com.enoch.leathercraft.validator.PasswordValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccountController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrderService orderService;
    private final MailService mailService;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));
    }

    // --- MON PROFIL ---
    @GetMapping("/me")
    public ProfileResponse getProfile() {
        User user = getCurrentUser();

        return ProfileResponse.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .addressLine1(user.getAddressLine1())
                .postalCode(user.getPostalCode())
                .city(user.getCity())
                .country(user.getCountry())
                .build();
    }

    @PutMapping("/me")
    public ProfileResponse updateProfile(@RequestBody ProfileUpdateRequest request) {
        User user = getCurrentUser();

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setAddressLine1(request.getAddressLine1());
        user.setPostalCode(request.getPostalCode());
        user.setCity(request.getCity());
        user.setCountry(request.getCountry());

        User saved = userRepository.save(user);

        return ProfileResponse.builder()
                .firstName(saved.getFirstName())
                .lastName(saved.getLastName())
                .email(saved.getEmail())
                .phone(saved.getPhone())
                .addressLine1(saved.getAddressLine1())
                .postalCode(saved.getPostalCode())
                .city(saved.getCity())
                .country(saved.getCountry())
                .build();
    }

    // --- MES COMMANDES ---
    @GetMapping("/me/orders")
    public List<OrderResponse> getMyOrders() {
        User user = getCurrentUser();
        return orderService.getUserOrders(user.getEmail());
    }

    // --- SÉCURITÉ : CHANGEMENT DE MOT DE PASSE ---
    @PostMapping("/me/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        User user = getCurrentUser();

        // 1. Vérifier ancien mot de passe
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            return ResponseEntity
                    .badRequest()
                    .body("Ancien mot de passe incorrect.");
        }

        // 2. Nouveau différent de l'ancien
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            return ResponseEntity
                    .badRequest()
                    .body("Le nouveau mot de passe doit être différent de l'ancien.");
        }

        // 3. Complexité
        if (!PasswordValidator.isStrongPassword(request.getNewPassword())) {
            return ResponseEntity
                    .badRequest()
                    .body("Le mot de passe doit contenir au minimum 8 caractères, une majuscule, un chiffre et un caractère spécial.");
        }

        // 4. Tout est OK -> on change
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        mailService.sendPasswordChangedEmail(user.getEmail());
        return ResponseEntity.ok("Mot de passe mis à jour.");
    }

    // --- SUPPRESSION DE MON COMPTE (SOFT DELETE) ---
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount() {
        User user = getCurrentUser();

        if (!user.isDeleted()) {
            user.setDeleted(true);
            userRepository.save(user);
        }

        // Tu peux éventuellement envoyer un mail ici :
        // mailService.sendAccountDeletedEmail(user.getEmail());

        return ResponseEntity.noContent().build(); // 204
    }
}
