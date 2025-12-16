package com.enoch.leathercraft.dto;

public record CheckoutRequest(
        String firstName,
        String lastName,
        String email,
        String phone,
        String street,
        String postalCode,
        String city,
        String country,
        String notes,
        String promoCode // ✅ AJOUTÉ ICI
) { }