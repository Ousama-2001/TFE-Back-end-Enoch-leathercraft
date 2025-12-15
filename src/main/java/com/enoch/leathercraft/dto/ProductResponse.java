// src/main/java/com/enoch/leathercraft/dto/ProductResponse.java
package com.enoch.leathercraft.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;
    private String sku;
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private String currency;
    private Integer weightGrams;
    private Boolean isActive;

    // ✅ compat front actuel
    private List<String> imageUrls;

    // 🔥 CRUD images admin
    private List<ProductImageResponse> images;

    private Integer stockQuantity;

    private Long segmentCategoryId;
    private Long typeCategoryId;

    private String segmentSlug;
    private String typeSlug;
}
