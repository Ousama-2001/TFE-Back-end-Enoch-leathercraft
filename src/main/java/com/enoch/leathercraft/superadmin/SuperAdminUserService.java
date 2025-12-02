// src/main/java/com/enoch/leathercraft/superadmin/SuperAdminUserService.java
package com.enoch.leathercraft.superadmin;

import com.enoch.leathercraft.auth.domain.Role;
import com.enoch.leathercraft.auth.domain.User;
import com.enoch.leathercraft.auth.repo.UserRepository;
import com.enoch.leathercraft.superadmin.dto.UserAdminDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SuperAdminUserService {

    private final UserRepository userRepository;

    /**
     * Liste tous les utilisateurs (soft-delete inclus ou non selon ton choix)
     */
    @Transactional(readOnly = true)
    public List<UserAdminDto> findAllUsers() {
        // ici je prends tous les users non soft-deleted
        return userRepository.findAll()
                .stream()
                .filter(u -> !u.isDeleted())
                .map(UserAdminDto::fromEntity)
                .toList();
    }

    /**
     * Change le rôle d'un utilisateur
     */
    @Transactional
    public UserAdminDto updateUserRole(Long userId, Role newRole, String currentEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));

        if (user.isDeleted()) {
            throw new IllegalStateException("Cet utilisateur est supprimé (soft delete).");
        }

        // Sécurité : ne pas permettre à un super admin de modifier son propre rôle
        if (user.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new IllegalStateException("Vous ne pouvez pas modifier votre propre rôle.");
        }

        user.setRole(newRole);
        userRepository.save(user);

        return UserAdminDto.fromEntity(user);
    }

    /**
     * Soft delete utilisateur (sauf soi-même)
     */
    @Transactional
    public void softDeleteUser(Long userId, String currentEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));

        if (user.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new IllegalStateException("Vous ne pouvez pas supprimer votre propre compte.");
        }

        if (user.isDeleted()) {
            return; // déjà supprimé
        }

        user.setDeleted(true);
        userRepository.save(user);
    }
}
