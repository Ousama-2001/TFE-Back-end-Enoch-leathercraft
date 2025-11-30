// src/main/java/com/enoch/leathercraft/entities/ReviewStatus.java
package com.enoch.leathercraft.entities;

public enum ReviewStatus {
    VISIBLE,   // Avis affiché au public
    HIDDEN,    // Masqué, visible seulement pour l’admin
    DELETED    // Soft delete (gardé en base, plus affiché)
}
