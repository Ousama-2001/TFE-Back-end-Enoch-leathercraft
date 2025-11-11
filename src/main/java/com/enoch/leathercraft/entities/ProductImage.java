// com/enoch/leathercraft/catalog/domain/ProductImage.java
package com.enoch.leathercraft.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="product_images")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductImage {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;

    @Column(name="product_id", nullable=false) private Long productId;

    @Column(nullable=false, length=500) private String url;

    private String altText;
    private Integer position;
    private Boolean isPrimary;
}
