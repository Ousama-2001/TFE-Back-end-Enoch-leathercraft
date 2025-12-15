package com.enoch.leathercraft.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactMessageAdminDto {
    private Long id;
    private String name;
    private String email;
    private String message;
    private LocalDateTime createdAt;

    private boolean handled;
    private LocalDateTime handledAt;
    private String handledBy;
}
