package com.enoch.leathercraft.services;

import com.enoch.leathercraft.dto.SalesStatsResponse;
import com.enoch.leathercraft.entities.Order;
import com.enoch.leathercraft.entities.OrderItem;
import com.enoch.leathercraft.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final OrderRepository orderRepository;

    public SalesStatsResponse getGlobalSalesStats() {
        List<Order> orders = orderRepository.findAll();

        long totalOrders = orders.size();
        long totalItems = 0L;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                long qty = item.getQuantity();
                totalItems += qty;

                BigDecimal lineTotal = item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(qty));
                totalRevenue = totalRevenue.add(lineTotal);
            }
        }

        BigDecimal avgOrderValue = BigDecimal.ZERO;
        if (totalOrders > 0) {
            avgOrderValue = totalRevenue
                    .divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
        }

        return SalesStatsResponse.builder()
                .totalOrders(totalOrders)
                .totalItemsSold(totalItems)
                .totalRevenue(totalRevenue)
                .averageOrderValue(avgOrderValue)
                .build();
    }
}
