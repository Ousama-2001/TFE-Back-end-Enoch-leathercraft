// src/main/java/com/enoch/leathercraft/entities/OrderStatus.java
package com.enoch.leathercraft.entities;

public enum OrderStatus {
    PENDING,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED,

    // 🔁 Gestion des retours
    RETURN_REQUESTED,
    RETURN_APPROVED,
    RETURN_REJECTED
}
