package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.dto.CheckoutRequest;
import com.enoch.leathercraft.dto.OrderResponse;
import com.enoch.leathercraft.dto.StripeCheckoutResponse;
import com.enoch.leathercraft.services.OrderService;
import com.enoch.leathercraft.services.StripeService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderService orderService;
    private final StripeService stripeService;

    @PostMapping("/stripe-checkout")
    public ResponseEntity<StripeCheckoutResponse> createStripeCheckout(
            Authentication authentication,
            @RequestBody CheckoutRequest request
    ) throws StripeException {

        String email = authentication.getName();

        OrderResponse order = orderService.createOrderFromCart(email, request);
        String checkoutUrl = stripeService.createCheckoutSession(order);

        StripeCheckoutResponse response = new StripeCheckoutResponse(checkoutUrl, order.getReference());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/stripe-confirm")
    public ResponseEntity<OrderResponse> confirmStripePayment(
            @RequestParam("session_id") String sessionId
    ) throws StripeException {

        OrderResponse order = orderService.markOrderAsPaidFromStripeSession(sessionId);
        return ResponseEntity.ok(order);
    }
}
