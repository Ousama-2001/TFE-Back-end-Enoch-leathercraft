// src/main/java/com/enoch/leathercraft/auth/dto/RegisterRequest.java
package com.enoch.leathercraft.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank private String firstname;
    @NotBlank private String lastname;
    @Email @NotBlank private String email;
    @NotBlank @Size(min=6) private String password;
}
