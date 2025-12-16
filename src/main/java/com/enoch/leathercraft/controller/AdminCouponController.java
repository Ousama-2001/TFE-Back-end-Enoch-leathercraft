package com.enoch.leathercraft.controllers;

import com.enoch.leathercraft.dto.CouponRequest;
import com.enoch.leathercraft.dto.CouponResponse;
import com.enoch.leathercraft.services.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    @GetMapping
    public List<CouponResponse> getAll() {
        return couponService.getAll();
    }

    @PostMapping
    public CouponResponse create(@RequestBody CouponRequest req) {
        return couponService.create(req);
    }

    @PutMapping("/{id}")
    public CouponResponse update(@PathVariable Long id, @RequestBody CouponRequest req) {
        return couponService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        couponService.delete(id);
    }
}
