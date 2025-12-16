package com.enoch.leathercraft.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CouponValidateRequest {
    private String code;
    private BigDecimal orderAmount; // total panier/commande avant remise
}
