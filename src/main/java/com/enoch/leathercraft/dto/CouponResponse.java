package com.enoch.leathercraft.dto;

import lombok.*;

import java.time.Instant;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class CouponResponse {
    private Long id;
    private String code;
    private Integer percent;
    private Instant startsAt;
    private Instant endsAt;
    private Boolean active;
    private Integer maxUses;
    private Integer usedCount;

    // utile pour l'admin + front
    private Boolean validNow;
    private String status; // ACTIVE / UPCOMING / EXPIRED / INACTIVE / LIMIT_REACHED
}
