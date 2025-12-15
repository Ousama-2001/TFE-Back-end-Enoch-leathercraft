package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.dto.OrderResponse;
import com.enoch.leathercraft.services.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    public record UpdateStatusRequest(@NotBlank String status) {}
    public record RejectReturnRequest(@NotBlank String reason) {}

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest body
    ) {
        return ResponseEntity.ok(orderService.updateStatus(id, body.status()));
    }

    @PostMapping("/{id}/returns/approve")
    public ResponseEntity<OrderResponse> approveReturn(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.approveReturnFromAdmin(id));
    }

    @PostMapping("/{id}/returns/reject")
    public ResponseEntity<OrderResponse> rejectReturn(
            @PathVariable Long id,
            @Valid @RequestBody RejectReturnRequest body
    ) {
        return ResponseEntity.ok(orderService.rejectReturnFromAdmin(id, body.reason()));
    }
}
