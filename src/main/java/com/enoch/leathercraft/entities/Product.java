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

    // 🔥 IMAGES
    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
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
}
