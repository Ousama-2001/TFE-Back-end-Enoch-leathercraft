package com.enoch.leathercraft.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CartItemResponse {
    private Long productId;
    private String name;
    private String sku;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineTotal;
    private String imageUrl; // <-- C'est souvent ce champ qui manque et cause l'erreur
}