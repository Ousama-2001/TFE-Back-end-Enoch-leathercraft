// src/main/java/com/enoch/leathercraft/dto/StripeCheckoutResponse.java
package com.enoch.leathercraft.dto;

public record StripeCheckoutResponse(
        String checkoutUrl,
        String orderReference
) {}
