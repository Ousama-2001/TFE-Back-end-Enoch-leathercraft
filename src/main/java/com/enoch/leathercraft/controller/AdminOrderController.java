// src/main/java/com/enoch/leathercraft/controller/AdminOrderController.java
package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.dto.OrderResponse;
import com.enoch.leathercraft.services.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    public record UpdateStatusRequest(String status) {}
    public record RejectReturnRequest(String reason) {}

    // Liste de toutes les commandes (admin)
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    // Détail d'une commande
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    // Mise à jour du statut (dropdown dans la modale)
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest body
    ) {
        OrderResponse updated = orderService.updateStatus(id, body.status());
        return ResponseEntity.ok(updated);
    }

    // Accepter un retour
    @PostMapping("/{id}/returns/approve")
    public ResponseEntity<OrderResponse> approveReturn(@PathVariable Long id) {
        OrderResponse updated = orderService.approveReturnFromAdmin(id);
        return ResponseEntity.ok(updated);
    }

    // Refuser un retour (raison obligatoire)
    @PostMapping("/{id}/returns/reject")
    public ResponseEntity<OrderResponse> rejectReturn(
            @PathVariable Long id,
            @RequestBody RejectReturnRequest body
    ) {
        OrderResponse updated = orderService.rejectReturnFromAdmin(id, body.reason());
        return ResponseEntity.ok(updated);
    }
}
