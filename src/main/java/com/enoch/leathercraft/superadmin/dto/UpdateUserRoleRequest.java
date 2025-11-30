package com.enoch.leathercraft.superadmin.dto;

public record UpdateUserRoleRequest(
        String role    // "CUSTOMER" | "ADMIN" | "SUPER_ADMIN"
) {
}
