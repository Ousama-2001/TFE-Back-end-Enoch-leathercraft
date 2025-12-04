package com.enoch.leathercraft.superadmin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "account_reactivation_requests")
@Getter
@Setter
@NoArgsConstructor
public class ReactivationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamp with time zone")
    private Instant createdAt;

    @Column(name = "handled", nullable = false)
    private boolean handled = false;

    @Column(name = "handled_at", columnDefinition = "timestamp with time zone")
    private Instant handledAt;

    @Column(name = "handled_by", length = 180)
    private String handledBy;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
