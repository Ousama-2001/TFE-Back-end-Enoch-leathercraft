// src/main/java/com/enoch/leathercraft/controller/AdminReviewController.java
package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.dto.AdminReviewResponse;
import com.enoch.leathercraft.entities.ReviewStatus;
import com.enoch.leathercraft.services.ProductReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ProductReviewService reviewService;

    /**
     * GET /api/admin/reviews
     * GET /api/admin/reviews?status=VISIBLE
     */
    @GetMapping
    public List<AdminReviewResponse> getAll(
            @RequestParam(name = "status", required = false) ReviewStatus status
    ) {
        return reviewService.adminGetAll(status);
    }

    /**
     * PATCH /api/admin/reviews/{id}/status/{status}
     * Exemple : /api/admin/reviews/10/status/HIDDEN
     */
    @PatchMapping("/{id}/status/{status}")
    public AdminReviewResponse changeStatus(
            @PathVariable Long id,
            @PathVariable ReviewStatus status
    ) {
        return reviewService.adminChangeStatus(id, status);
    }
}
