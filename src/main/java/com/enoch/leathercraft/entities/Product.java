// src/main/java/com/enoch/leathercraft/entities/Product.java
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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// ⬇️ IMPORTANT : on dit à Jackson d'ignorer les champs internes d'Hibernate
@JsonIgnoreProperties(value = {"hibernateLazyInitializer", "handler"}, ignoreUnknown = true)
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

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(length = 120)
    private String material;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency; // ex: "EUR"

    @Column(name = "weight_grams")
    private Integer weightGrams;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // ✅ Soft delete
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(nullable = false)
    private Integer stockQuantity = 0;

    // --- RELATION IMAGES ---
    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private Set<ProductImage> images = new HashSet<>();

    public void addImage(ProductImage image) {
        images.add(image);
        image.setProduct(this);
    }

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
