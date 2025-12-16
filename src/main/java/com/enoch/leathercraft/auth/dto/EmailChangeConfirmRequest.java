// src/main/java/com/enoch/leathercraft/auth/dto/EmailChangeConfirmRequest.java
package com.enoch.leathercraft.auth.dto;

import lombok.Data;

@Data
public class EmailChangeConfirmRequest {
    private String token;
}
