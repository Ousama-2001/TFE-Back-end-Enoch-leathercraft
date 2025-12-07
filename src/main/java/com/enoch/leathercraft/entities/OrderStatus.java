package com.enoch.leathercraft.entities;

public enum OrderStatus {
    PENDING,    // En attente
    PAID,       // Payé (si tu gères le paiement direct)
    SHIPPED,    // Expédié
    DELIVERED,  // Livré (C'est celui-ci qui doit exister pour que le bouton marche)
    CANCELLED,   // Annulé
    RETURN_REQUESTED
    }