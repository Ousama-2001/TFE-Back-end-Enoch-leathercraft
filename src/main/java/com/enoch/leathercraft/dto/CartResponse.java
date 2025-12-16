package com.enoch.leathercraft.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class CartResponse {
    private Long cartId;
    private List<CartItemResponse> items;
    private int totalQuantity;
    private BigDecimal totalAmount;
    private Instant expiresAt;

    // ✅ AJOUTS
    private String couponCode;
    private Integer discountPercent;
}