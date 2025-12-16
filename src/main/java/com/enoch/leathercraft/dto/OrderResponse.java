package com.enoch.leathercraft.dto;

import com.enoch.leathercraft.entities.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class OrderResponse {
    private Long id;
    private String reference;

    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    private String couponCode;
    private Integer couponPercent;

    private OrderStatus status;
    private Instant createdAt;

    private String notes;
    private List<OrderItemResponse> items;

    // ✅ AJOUTE CES DEUX CHAMPS
    private String customerEmail;
    private Long userId;
}