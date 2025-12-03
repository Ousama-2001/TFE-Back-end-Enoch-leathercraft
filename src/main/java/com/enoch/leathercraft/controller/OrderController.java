package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.dto.CheckoutRequest;
import com.enoch.leathercraft.dto.OrderResponse;
import com.enoch.leathercraft.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Valider le panier et créer une commande
     * POST /api/orders/checkout
     */
    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            Authentication authentication,
            @RequestBody CheckoutRequest request
    ) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.createOrderFromCart(email, request));
    }

    /**
     * Récupérer l'historique des commandes de l'utilisateur connecté
     * GET /api/orders/my-orders
     */
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.getUserOrders(email));
    }

    /**
     * Détail d'une commande appartenant à l'utilisateur connecté
     * GET /api/orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getMyOrderById(
            Authentication authentication,
            @PathVariable Long id
    ) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.getUserOrderById(id, email));
    }
}
