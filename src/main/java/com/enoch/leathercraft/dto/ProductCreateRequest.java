// src/main/java/com/enoch/leathercraft/dto/ProductCreateRequest.java
package com.enoch.leathercraft.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCreateRequest {

    private String sku;
    private String name;
    private String slug;
    private String description;
    private String material;
    private BigDecimal price;
    private String currency;
    private Integer weightGrams;
    private Boolean isActive;
    private Integer stockQuantity;

    // 🔥 Catégories choisies dans le back-office
    // segment = homme / femme / petite-maroquinerie
    private Long segmentCategoryId;

    // type = sacs-sacoches / ceintures / portefeuilles...
    private Long typeCategoryId;
}
