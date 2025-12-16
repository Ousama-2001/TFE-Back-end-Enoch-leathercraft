// src/main/java/com/enoch/leathercraft/auth/domain/EmailChangeToken.java
package com.enoch.leathercraft.auth.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Entity
@Table(name = "email_change_tokens")
@Getter @Setter
@NoArgsConstructor
public class EmailChangeToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String token;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @Column(name = "new_email", nullable = false, length = 180)
    private String newEmail;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static EmailChangeToken forUser(User user, String newEmail, int minutes) {
        EmailChangeToken t = new EmailChangeToken();
        t.user = user;
        t.newEmail = newEmail;
        t.createdAt = Instant.now();
        t.expiresAt = t.createdAt.plusSeconds(minutes * 60L);
        t.token = generateToken();
        return t;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    private static String generateToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
