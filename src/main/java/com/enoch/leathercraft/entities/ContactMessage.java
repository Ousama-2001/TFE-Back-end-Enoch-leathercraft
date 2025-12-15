package com.enoch.leathercraft.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contact_message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(nullable = false, length = 4000)
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // ✅ suivi superadmin
    @Column(nullable = false)
    private boolean handled;

    private LocalDateTime handledAt;

    @Column(length = 180)
    private String handledBy;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        handled = false;
    }
}
