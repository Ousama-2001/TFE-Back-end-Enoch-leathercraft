package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.auth.domain.Role;
import com.enoch.leathercraft.auth.domain.User;
import com.enoch.leathercraft.auth.repo.UserRepository;
import com.enoch.leathercraft.dto.AdminUserSummaryDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin/users")
@RequiredArgsConstructor
public class SuperAdminUserController {

    private final UserRepository userRepository;

    /**
     * GET /api/super-admin/users
     * Liste tous les utilisateurs
     */
    @GetMapping
    public ResponseEntity<List<AdminUserSummaryDto>> getAllUsers() {
        List<AdminUserSummaryDto> users = userRepository.findAll().stream()
                .map(AdminUserSummaryDto::fromEntity)
                .toList();

        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/super-admin/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminUserSummaryDto> getUser(@PathVariable Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));

        return ResponseEntity.ok(AdminUserSummaryDto.fromEntity(u));
    }

    /**
     * PATCH /api/super-admin/users/{id}/role?value=ADMIN
     * Change le rôle d'un utilisateur
     */
    @PatchMapping("/{id}/role")
    public ResponseEntity<AdminUserSummaryDto> changeRole(
            @PathVariable Long id,
            @RequestParam("value") String value
    ) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));

        Role newRole = Role.valueOf(value.toUpperCase()); // CUSTOMER, ADMIN, SUPER_ADMIN

        u.setRole(newRole);
        userRepository.save(u);

        return ResponseEntity.ok(AdminUserSummaryDto.fromEntity(u));
    }

    /**
     * DELETE /api/super-admin/users/{id}
     * Pour l'instant : hard delete (on pourra passer en soft delete plus tard si tu veux)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("Utilisateur introuvable");
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
