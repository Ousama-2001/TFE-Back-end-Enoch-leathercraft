package com.enoch.leathercraft.dto;

import com.enoch.leathercraft.auth.domain.Role;
import com.enoch.leathercraft.auth.domain.User;

import java.time.Instant;

public record AdminUserSummaryDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        String username,
        Role role,
        Instant createdAt
) {

    public static AdminUserSummaryDto fromEntity(User u) {
        return new AdminUserSummaryDto(
                u.getId(),
                u.getEmail(),
                u.getFirstName(),
                u.getLastName(),
                u.getUsername(),
                u.getRole(),
                u.getCreatedAt()
        );
    }
}
