package com.enoch.leathercraft.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    // ✅ PROMO (DATE SEULEMENT)
    private BigDecimal promoPrice;
    private LocalDate promoStartAt; // yyyy-MM-dd
    private LocalDate promoEndAt;   // yyyy-MM-dd

    // 🔥 Catégories
    private Long segmentCategoryId;
    private Long typeCategoryId;
}
