// src/main/java/com/enoch/leathercraft/superadmin/dto/ReactivationRequestDto.java
package com.enoch.leathercraft.superadmin.dto;

import com.enoch.leathercraft.superadmin.ReactivationRequest;

import java.time.Instant;

public record ReactivationRequestDto(
        Long id,
        String email,
        String message,
        Instant createdAt,
        boolean handled,
        Instant handledAt,
        String handledBy
) {
    public static ReactivationRequestDto fromEntity(ReactivationRequest r) {
        return new ReactivationRequestDto(
                r.getId(),
                r.getEmail(),
                r.getMessage(),
                r.getCreatedAt(),
                r.isHandled(),
                r.getHandledAt(),
                r.getHandledBy()
        );
    }
}
