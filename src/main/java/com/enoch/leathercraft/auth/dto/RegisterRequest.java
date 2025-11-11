package com.enoch.leathercraft.auth.dto;
public record RegisterRequest(
        String email,
        String password,
        String firstName,
        String lastName,
        String username   // NEW
) {}