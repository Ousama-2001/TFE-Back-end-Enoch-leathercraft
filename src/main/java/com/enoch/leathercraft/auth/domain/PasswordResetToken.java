package com.enoch.leathercraft.auth.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    public static PasswordResetToken forUser(User user, long validityMinutes) {
        PasswordResetToken t = new PasswordResetToken();
        t.setUser(user);
        t.setToken(UUID.randomUUID().toString());
        t.setExpiresAt(Instant.now().plusSeconds(validityMinutes * 60));
        return t;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
