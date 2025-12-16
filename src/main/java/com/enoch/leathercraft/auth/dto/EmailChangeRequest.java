// src/main/java/com/enoch/leathercraft/auth/dto/EmailChangeRequest.java
package com.enoch.leathercraft.auth.dto;

import lombok.Data;

@Data
public class EmailChangeRequest {
    private String newEmail;
    private String currentPassword; // optionnel mais recommandé
}
