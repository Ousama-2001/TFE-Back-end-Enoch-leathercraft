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
     * Liste tous les utilisateurs (pour la page d'administration)
     */
    @Transactional(readOnly = true)
    public List<UserAdminDto> findAllUsers() {
        return userRepository.findAll()
                .stream()
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

        // Sécurité : ne pas permettre à un super admin de modifier son propre rôle
        if (user.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new IllegalStateException("Vous ne pouvez pas modifier votre propre rôle.");
        }

        user.setRole(newRole);
        return UserAdminDto.fromEntity(user);
    }

    /**
     * Supprime un utilisateur (sauf soi-même)
     */
    @Transactional
    public void deleteUser(Long userId, String currentEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));

        // Sécurité : ne pas se supprimer soi-même
        if (user.getEmail().equalsIgnoreCase(currentEmail)) {
            throw new IllegalStateException("Vous ne pouvez pas supprimer votre propre compte.");
        }

        userRepository.delete(user);
    }
}
