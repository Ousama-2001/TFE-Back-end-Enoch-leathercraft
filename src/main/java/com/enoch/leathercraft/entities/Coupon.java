package com.enoch.leathercraft.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "coupons")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 80)
    private String code;

    // ex: 10 = -10%
    @Column(nullable = false)
    private Integer percent;

    private Instant startsAt; // nullable
    private Instant endsAt;   // nullable

    @Builder.Default
    private Boolean active = true;

    private Integer maxUses; // nullable (illimité si null)
    @Builder.Default
    private Integer usedCount = 0;

    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
        if (active == null) active = true;
        if (usedCount == null) usedCount = 0;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
