package com.enoch.leathercraft.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="product_images")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductImage {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    // --- MODIFICATION MAJEURE : Remplacement de Long productId par l'entité Product ---

    // Relation Many-to-One avec Product (plusieurs images pour un seul produit)
    // Le champ `name = "product_id"` crée la colonne de clé étrangère dans cette table.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // --- FIN MODIFICATION ---


    @Column(nullable=false, length=500)
    private String url; // Le chemin d'accès public (ex: /uploads/products/image.jpg)

    private String altText;

    // Initialiser la position et l'isPrimary pour éviter les NullPointers
    @Builder.Default
    private Integer position = 0;

    @Builder.Default
    private Boolean isPrimary = false;
}