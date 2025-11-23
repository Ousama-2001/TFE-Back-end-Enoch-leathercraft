package com.enoch.leathercraft.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {
    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;
}