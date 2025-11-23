package com.enoch.leathercraft.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 100)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(unique = true, length = 180)
    private String slug;

    @Lob
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

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // --- NOUVEAUTÉS POUR LA RELATION IMAGE ---

    // Relation One-to-Many avec ProductImage
    // CascadeType.ALL assure que lorsque vous sauvez/supprimez le produit, les images associées sont aussi gérées.
    // orphanRemoval = true assure que si une image est retirée de la collection, elle est supprimée de la DB.
    // "mappedBy" indique que c'est l'entité ProductImage qui détient la clé étrangère (champ 'product').
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default // Utiliser la valeur par défaut Lombok avec le pattern Builder
    private Set<ProductImage> images = new HashSet<>();

    // Méthode utilitaire pour maintenir la cohérence bi-directionnelle (bonne pratique JPA)
    public void addImage(ProductImage image) {
        images.add(image);
        image.setProduct(this);
    }

    // --- FIN NOUVEAUTÉS ---


    @PrePersist
    void onCreate() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}