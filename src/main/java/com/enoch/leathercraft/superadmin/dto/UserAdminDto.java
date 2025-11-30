package com.enoch.leathercraft.superadmin.dto;

import com.enoch.leathercraft.auth.domain.Role;
import com.enoch.leathercraft.auth.domain.User;

import java.time.Instant;

public record UserAdminDto(
        Long id,
        String email,
        String username,
        String firstName,
        String lastName,
        Role role,
        Instant createdAt
) {
    public static UserAdminDto fromEntity(User u) {
        return new UserAdminDto(
                u.getId(),
                u.getEmail(),
                u.getUsername(),
                u.getFirstName(),
                u.getLastName(),
                u.getRole(),
                u.getCreatedAt()
        );
    }
}
