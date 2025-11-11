package com.enoch.leathercraft.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class ProductCreateRequest {
    private String sku;
    private String name;
    private String slug;
    private String description;
    private String material;
    private BigDecimal price;  // DECIMAL(10,2)
    private String currency;   // "EUR"
    private Integer weightGrams;
    private Boolean isActive;
}
