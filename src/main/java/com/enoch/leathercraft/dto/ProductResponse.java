package com.enoch.leathercraft.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    // ✅ promo brute (optionnelle) : DATE SEULEMENT
    private BigDecimal promoPrice;
    private LocalDate promoStartAt;
    private LocalDate promoEndAt;

    private String currency;
    private Integer weightGrams;
    private Boolean isActive;

    private List<String> imageUrls;
    private List<ProductImageResponse> images;

    private Integer stockQuantity;

    private Long segmentCategoryId;
    private Long typeCategoryId;

    private String segmentSlug;
    private String typeSlug;
}
