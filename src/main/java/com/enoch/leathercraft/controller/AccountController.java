package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.auth.domain.User;
import com.enoch.leathercraft.auth.repo.UserRepository;
import com.enoch.leathercraft.dto.ChangePasswordRequest;
import com.enoch.leathercraft.dto.ProfileResponse;
import com.enoch.leathercraft.dto.ProfileUpdateRequest;
import com.enoch.leathercraft.dto.OrderResponse;


import com.enoch.leathercraft.services.OrderService;
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

        // 🔽 Utiliser passwordHash
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest().body("Ancien mot de passe incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok().build();
    }

}
