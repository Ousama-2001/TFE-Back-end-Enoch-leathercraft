package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.dto.CheckoutRequest;
import com.enoch.leathercraft.dto.OrderResponse;
import com.enoch.leathercraft.dto.ReturnRequest;
import com.enoch.leathercraft.dto.StripeCheckoutResponse;
import com.enoch.leathercraft.services.OrderService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
        return ResponseEntity.ok(orderService.cancelOrder(id, email));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<StripeCheckoutResponse> payOrder(
            Authentication authentication,
            @PathVariable Long id
    ) throws StripeException {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.createStripeCheckoutForOrder(id, email));
    }

    @PostMapping("/{id}/return")
    public ResponseEntity<OrderResponse> requestReturn(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody ReturnRequest request
    ) {
        String email = authentication.getName();
        return ResponseEntity.ok(orderService.requestReturn(id, email, request));
    }

    // ✅ FACTURE PDF
    @GetMapping("/{id}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(
            Authentication authentication,
            @PathVariable Long id
    ) {
        String email = authentication.getName();
        byte[] pdf = orderService.generateInvoicePdf(id, email);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("invoice-" + id + ".pdf")
                        .build()
        );

        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    // ✅ BON DE RETOUR PDF
    @GetMapping("/{id}/return-label")
    public ResponseEntity<byte[]> downloadReturnLabel(
            Authentication authentication,
            @PathVariable Long id
    ) {
        String email = authentication.getName();
        byte[] pdf = orderService.generateReturnLabelPdf(id, email);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename("return-label-" + id + ".pdf")
                        .build()
        );

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
