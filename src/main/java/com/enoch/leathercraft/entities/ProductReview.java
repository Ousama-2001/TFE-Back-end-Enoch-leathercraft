// src/main/java/com/enoch/leathercraft/entities/ProductReview.java
package com.enoch.leathercraft.entities;

import com.enoch.leathercraft.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "product_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Produit concerné
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Auteur (user)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Nom affiché
    @Column(name = "author_name", length = 150)
    private String authorName;

    private int rating; // 1..5

    @Column(columnDefinition = "text")
    private String comment;

    // --------- MODÉRATION / SOFT DELETE ---------
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.VISIBLE;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // --------- DATES ---------
    @Column(name = "created_at", columnDefinition = "timestamp with time zone")
    private Instant createdAt;

    @Column(name = "updated_at", columnDefinition = "timestamp with time zone")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
