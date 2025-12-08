// src/main/java/com/enoch/leathercraft/entities/Category.java
package com.enoch.leathercraft.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // slug technique : "homme", "femme", "petite-maroquinerie",
    // "sacs-sacoches", "ceintures", "portefeuilles", etc.
    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    // Libellé affiché : "Homme", "Sacs & sacoches", etc.
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    // Pour une éventuelle arborescence (non utilisé pour l'instant)
    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
