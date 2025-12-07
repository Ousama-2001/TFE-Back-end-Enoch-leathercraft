// src/main/java/com/enoch/leathercraft/dto/ReturnRequest.java
package com.enoch.leathercraft.dto;

public record ReturnRequest(
        String reason,   // motif choisi (ex: "Taille incorrecte")
        String comment   // commentaire libre optionnel
) {
}
