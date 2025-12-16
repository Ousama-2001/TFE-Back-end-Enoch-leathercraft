// src/main/java/com/enoch/leathercraft/dto/CouponValidateResponse.java
package com.enoch.leathercraft.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponValidateResponse {
    private String code;
    private boolean valid;
    private Integer percent; // null si invalid
    private String reason;   // optionnel
}
