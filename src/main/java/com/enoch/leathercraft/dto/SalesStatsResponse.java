package com.enoch.leathercraft.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SalesStatsResponse {
    private long totalOrders;
    private long totalItemsSold;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
}
