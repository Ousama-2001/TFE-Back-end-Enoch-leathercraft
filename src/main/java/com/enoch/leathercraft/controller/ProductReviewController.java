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
    public List<ProductReviewResponse> getForProduct(@PathVariable Long productId) {
        return reviewService.getReviewsForProduct(productId);
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
}
