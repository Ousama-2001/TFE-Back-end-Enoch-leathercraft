package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.dto.CouponValidateResponse;
import com.enoch.leathercraft.services.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/validate")
    public CouponValidateResponse validate(@RequestParam String code) {
        return couponService.validate(code);
    }
}
