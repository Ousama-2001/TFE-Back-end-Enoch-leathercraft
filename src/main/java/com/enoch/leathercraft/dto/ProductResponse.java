// src/main/java/com/enoch/leathercraft/dto/ProductResponse.java
package com.enoch.leathercraft.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private String sku;
    private String name;
    private String slug;
    private String description;
    private String material;
    private BigDecimal price;
    private String currency;
    private Integer weightGrams;
    private Boolean isActive;
    private List<String> imageUrls;
    private Integer stockQuantity;

    // 🔥 Pour pré-remplir le formulaire admin
    private Long segmentCategoryId;
    private Long typeCategoryId;

    // 🔥 Très utile pour le front catalogue
    private String segmentSlug;  // ex: "homme"
    private String typeSlug;     // ex: "sacs-sacoches"
}
