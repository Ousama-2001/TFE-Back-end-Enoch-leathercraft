package com.enoch.leathercraft.superadmin.dto;

import com.enoch.leathercraft.superadmin.ReactivationRequest;

import java.time.Instant;

public record ReactivationRequestDto(
        Long id,
        String email,
        Instant createdAt,
        boolean handled,
        Instant handledAt,
        String handledBy
) {
    public static ReactivationRequestDto fromEntity(ReactivationRequest r) {
        return new ReactivationRequestDto(
                r.getId(),
                r.getEmail(),
                r.getCreatedAt(),
                r.isHandled(),
                r.getHandledAt(),
                r.getHandledBy()
        );
    }
}
