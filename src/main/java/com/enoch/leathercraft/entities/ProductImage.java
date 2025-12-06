// src/main/java/com/enoch/leathercraft/entities/ProductImage.java
package com.enoch.leathercraft.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="product_images")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore              // 🔴 Évite la boucle ProductImage -> Product -> images -> ...
    private Product product;

    @Column(nullable=false, length=500)
    private String url;

    private String altText;

    @Builder.Default
    private Integer position = 0;

    @Builder.Default
    private Boolean isPrimary = false;
}
