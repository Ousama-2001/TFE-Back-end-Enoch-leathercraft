package com.enoch.leathercraft.dto;

public record StripeCheckoutResponse(
        String checkoutUrl,
        String orderReference
) { }
