// src/main/java/com/enoch/leathercraft/controller/OrderController.java
package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.dto.CheckoutRequest;
import com.enoch.leathercraft.dto.OrderResponse;
import com.enoch.leathercraft.dto.StripeCheckoutResponse;
import com.enoch.leathercraft.dto.ReturnRequest;
import com.enoch.leathercraft.services.OrderService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            Authentication authentication,
            @RequestBody CheckoutRequest request
    ) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.createOrderFromCart(email, request));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.getUserOrders(email));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getMyOrderById(
            Authentication authentication,
            @PathVariable Long id
    ) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.getUserOrderById(id, email));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            Authentication authentication,
            @PathVariable Long id
    ) {
        String email = authentication.getName();
        OrderResponse updated = orderService.cancelOrder(id, email);
        return ResponseEntity.ok(updated);
    }

    /**
     * Payer une commande en attente : crée une session Stripe
     * et renvoie l'URL de checkout.
     */
    @PostMapping("/{id}/pay")
    public ResponseEntity<StripeCheckoutResponse> payOrder(
            Authentication authentication,
            @PathVariable Long id
    ) throws StripeException {
        String email = authentication.getName();
        StripeCheckoutResponse resp = orderService.createStripeCheckoutForOrder(id, email);
        return ResponseEntity.ok(resp);
    }

    /**
     * 🔥 Demande de retour sur une commande livrée
     */
    @PostMapping("/{id}/return")
    public ResponseEntity<OrderResponse> requestReturn(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody ReturnRequest request
    ) {
        String email = authentication.getName();
        OrderResponse updated = orderService.requestReturn(id, email, request);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(
            Authentication authentication,
            @PathVariable Long id
    ) {
        String email = authentication.getName();
        String invoiceText = orderService.generateInvoiceContent(id, email);

        byte[] bytes = invoiceText.getBytes(StandardCharsets.UTF_8);
        String filename = "invoice-" + id + ".txt";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(filename).build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(bytes);
    }
}
