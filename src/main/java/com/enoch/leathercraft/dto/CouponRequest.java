package com.enoch.leathercraft.dto;

import java.time.Instant;

public record CouponRequest(
        String code,
        Integer percent,
        Instant startsAt,
        Instant endsAt,
        Boolean active,
        Integer maxUses
) {}
