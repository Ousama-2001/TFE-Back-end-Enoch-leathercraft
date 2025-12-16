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

    // ✅ montants
    private BigDecimal subtotalAmount;   // total items avant remise
    private BigDecimal discountAmount;   // remise
    private BigDecimal totalAmount;      // total final (doit matcher Stripe)

    // ✅ coupon (si utilisé)
    private String couponCode;
    private Integer couponPercent;

    private OrderStatus status;
    private Instant createdAt;

    private String notes;
    private List<OrderItemResponse> items;
}
