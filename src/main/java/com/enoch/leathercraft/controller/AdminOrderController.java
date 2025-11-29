package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.dto.OrderResponse;
import com.enoch.leathercraft.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    /**
     * Liste de toutes les commandes (admin)
     * GET /api/admin/orders
     */
    @GetMapping
    public List<OrderResponse> getAll() {
        return orderService.getAllOrders();
    }

    /**
     * Détail d'une commande (admin)
     * GET /api/admin/orders/{id}
     */
    @GetMapping("/{id}")
    public OrderResponse getOne(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }

    /**
     * Mise à jour du statut d'une commande (admin)
     * PATCH /api/admin/orders/{id}/status?status=SHIPPED
     */
    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(
            @PathVariable Long id,
            @RequestParam("status") String status
    ) {
        return orderService.updateStatus(id, status);
    }
}
