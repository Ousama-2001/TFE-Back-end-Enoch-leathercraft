// src/main/java/com/enoch/leathercraft/dto/OrderResponse.java
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
    private BigDecimal totalAmount;
    private OrderStatus status;
    private Instant createdAt;
    private String notes;                 // 🆕 pour afficher motif / décisions
    private List<OrderItemResponse> items;
}
