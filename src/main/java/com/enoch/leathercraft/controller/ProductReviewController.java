package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.dto.ProductReviewCreateRequest;
import com.enoch.leathercraft.dto.ProductReviewResponse;
import com.enoch.leathercraft.services.ProductReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService reviewService;

    /**
     * Récupérer les avis pour un produit
     * GET /api/product-reviews/product/{productId}
     */
    @GetMapping("/product/{productId}")
    public List<ProductReviewResponse> getForProduct(@PathVariable Long productId,
                                                     Authentication authentication) {
        String email = (authentication != null) ? authentication.getName() : null;
        return reviewService.getReviewsForProduct(productId, email);
    }

    /**
     * Ajouter un avis pour le produit (utilisateur connecté)
     * POST /api/product-reviews
     */
    @PostMapping
    public ProductReviewResponse addReview(Authentication authentication,
                                           @RequestBody ProductReviewCreateRequest request) {
        String email = authentication.getName();
        return reviewService.addReview(email, request);
    }

    /**
     * Modifier un avis (propriétaire uniquement)
     * PUT /api/product-reviews/{id}
     */
    @PutMapping("/{id}")
    public ProductReviewResponse updateReview(Authentication authentication,
                                              @PathVariable Long id,
                                              @RequestBody ProductReviewCreateRequest request) {
        String email = authentication.getName();
        return reviewService.updateReview(email, id, request);
    }

    /**
     * Supprimer un avis (propriétaire uniquement)
     * DELETE /api/product-reviews/{id}
     */
    @DeleteMapping("/{id}")
    public void deleteReview(Authentication authentication,
                             @PathVariable Long id) {
        String email = authentication.getName();
        reviewService.deleteReview(email, id);
    }
}
