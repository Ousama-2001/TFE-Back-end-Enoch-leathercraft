package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.dto.SalesStatsResponse;
import com.enoch.leathercraft.services.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsService statsService;

    @GetMapping("/sales")
    public SalesStatsResponse getSalesStats() {
        return statsService.getGlobalSalesStats();
    }
}
