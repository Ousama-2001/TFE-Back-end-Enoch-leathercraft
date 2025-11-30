// src/main/java/com/enoch/leathercraft/superadmin/SuperAdminReviewController.java
package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.dto.AdminReviewResponse;
import com.enoch.leathercraft.services.ProductReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin/reviews")
@RequiredArgsConstructor
public class SuperAdminReviewController {

    private final ProductReviewService reviewService;

    /**
     * GET /api/super-admin/reviews/deleted
     * -> liste les avis soft-supprimés
     */
    @GetMapping("/deleted")
    public List<AdminReviewResponse> getDeleted() {
        return reviewService.superAdminGetDeleted();
    }

    /**
     * PATCH /api/super-admin/reviews/{id}/restore
     * -> restaure un avis soft-supprimé
     */
    @PatchMapping("/{id}/restore")
    public AdminReviewResponse restore(@PathVariable Long id) {
        return reviewService.superAdminRestore(id);
    }

    /**
     * DELETE /api/super-admin/reviews/{id}
     * -> suppression DEFINITIVE en base
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> hardDelete(@PathVariable Long id) {
        reviewService.superAdminHardDelete(id);
        return ResponseEntity.noContent().build(); // 204
    }

}
