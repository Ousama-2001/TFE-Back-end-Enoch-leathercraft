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
     * GET /api/admin/reviews?status=VISIBLE|HIDDEN|DELETED (optionnel)
     * -> liste des avis pour l'admin
     */
    @GetMapping
    public List<AdminReviewResponse> getAll(
            @RequestParam(value = "status", required = false) ReviewStatus status
    ) {
        // si status == null => tous les statuts
        return reviewService.adminGetAll(status);
    }

    /**
     * PATCH /api/admin/reviews/{id}/status?status=VISIBLE|HIDDEN|DELETED
     * -> change le statut d’un avis
     */
    @PatchMapping("/{id}/status")
    public AdminReviewResponse changeStatus(
            @PathVariable Long id,
            @RequestParam("status") ReviewStatus status
    ) {
        return reviewService.adminChangeStatus(id, status);
    }
}
