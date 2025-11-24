// com.enoch.leathercraft.auth.dto.AuthRequest.java
package com.enoch.leathercraft.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    // On accepte "identifier" ET "email" depuis le JSON
    @JsonAlias({"identifier", "email"})
    private String identifier;

    private String password;
}