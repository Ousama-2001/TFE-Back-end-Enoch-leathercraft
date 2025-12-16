package com.enoch.leathercraft.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "products")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 100)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(unique = true, length = 180)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String material;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    private Integer weightGrams;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private boolean deleted = false;

    private Instant createdAt;
    private Instant updatedAt;

    @Builder.Default
    private Integer stockQuantity = 0;

    // ✅ PROMO
    @Column(precision = 10, scale = 2)
    private BigDecimal promoPrice;      // nullable
    private Instant promoStartAt;       // nullable
    private Instant promoEndAt;         // nullable

    // ✅ CODE PROMO (optionnel)
    @Column(length = 40)
    private String promoCode;

    // 🔥 IMAGES
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ProductImage> images = new HashSet<>();

    public void addImage(ProductImage image) {
        images.add(image);
        image.setProduct(this);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // ========================================================================
    // ✅ LOGIQUE PROMO AJOUTÉE ICI
    // ========================================================================

    /**
     * Vérifie si la promotion est active maintenant (dates + prix cohérent).
     */
    public boolean isPromoValid() {
        // 1. Vérifier si un prix promo existe et est positif
        if (promoPrice == null || promoPrice.compareTo(BigDecimal.ZERO) <= 0) return false;

        // 2. Vérifier que la promo est bien inférieure au prix de base
        if (promoPrice.compareTo(price) >= 0) return false;

        // 3. Vérifier les dates
        Instant now = Instant.now();
        boolean started = promoStartAt == null || !now.isBefore(promoStartAt);
        boolean notEnded = promoEndAt == null || !now.isAfter(promoEndAt);

        return started && notEnded;
    }

    /**
     * Retourne le VRAI prix à payer (Base ou Promo).
     */
    public BigDecimal getEffectivePrice() {
        if (isPromoValid()) {
            return promoPrice;
        }
        return price;
    }
}