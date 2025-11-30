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

    @GetMapping("/product/{productId}")
    public List<ProductReviewResponse> getForProduct(@PathVariable Long productId,
                                                     Authentication authentication) {
        String email = (authentication != null) ? authentication.getName() : null;
        return reviewService.getReviewsForProduct(productId, email);
    }

    @PostMapping
    public ProductReviewResponse addReview(Authentication authentication,
                                           @RequestBody ProductReviewCreateRequest request) {
        String email = authentication.getName();
        return reviewService.addReview(email, request);
    }

    @PutMapping("/{id}")
    public ProductReviewResponse updateReview(Authentication authentication,
                                              @PathVariable Long id,
                                              @RequestBody ProductReviewCreateRequest request) {
        String email = authentication.getName();
        return reviewService.updateReview(email, id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteReview(Authentication authentication,
                             @PathVariable Long id) {
        String email = authentication.getName();
        reviewService.deleteReview(email, id);
    }
}
