// com/enoch/leathercraft/catalog/domain/ProductCategory.java
package com.enoch.leathercraft.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="product_categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(ProductCategoryId.class)
public class ProductCategory {
    @Id @Column(name="product_id")  private Long productId;
    @Id @Column(name="category_id") private Long categoryId;

    private Boolean isPrimary;
}
