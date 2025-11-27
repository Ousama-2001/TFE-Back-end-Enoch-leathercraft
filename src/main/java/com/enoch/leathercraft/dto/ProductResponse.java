package com.enoch.leathercraft.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List; // Import nécessaire

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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

}