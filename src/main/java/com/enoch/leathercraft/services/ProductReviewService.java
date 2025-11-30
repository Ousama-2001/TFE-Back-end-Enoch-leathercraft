package com.enoch.leathercraft.services;

import com.enoch.leathercraft.auth.domain.User;
import com.enoch.leathercraft.auth.repo.UserRepository;
import com.enoch.leathercraft.dto.ProductReviewCreateRequest;
import com.enoch.leathercraft.dto.ProductReviewResponse;
import com.enoch.leathercraft.entities.Product;
import com.enoch.leathercraft.entities.ProductReview;
import com.enoch.leathercraft.repository.ProductRepository;
import com.enoch.leathercraft.repository.ProductReviewRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductReviewService {

    private final ProductReviewRepository reviewRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    // ============ CRÉATION ============
    public ProductReviewResponse addReview(String userEmail, ProductReviewCreateRequest req) {

        Product product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));

        ProductReview review = ProductReview.builder()
                .product(product)
                .user(user)
                .authorName(buildAuthorName(user))
                .rating(req.getRating())
                .comment(req.getComment())
                .build();

        review = reviewRepo.save(review);
        // L'utilisateur connecté = auteur => mine = true
        return toDto(review, true);
    }

    // ============ LECTURE ============
    @Transactional(readOnly = true)
    public List<ProductReviewResponse> getReviewsForProduct(Long productId, String currentUserEmail) {
        return reviewRepo.findByProduct_IdOrderByCreatedAtDesc(productId)
                .stream()
                .map(r -> {
                    boolean mine = false;
                    if (currentUserEmail != null && r.getUser() != null && r.getUser().getEmail() != null) {
                        mine = currentUserEmail.equalsIgnoreCase(r.getUser().getEmail());
                    }
                    return toDto(r, mine);
                })
                .toList();
    }

    // ============ MISE À JOUR ============
    public ProductReviewResponse updateReview(String userEmail,
                                              Long reviewId,
                                              ProductReviewCreateRequest req) {

        ProductReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Avis introuvable"));

        if (review.getUser() == null || review.getUser().getEmail() == null ||
                !review.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("Vous ne pouvez modifier que vos propres avis.");
        }

        review.setRating(req.getRating());
        review.setComment(req.getComment());

        review = reviewRepo.save(review);
        return toDto(review, true);
    }

    // ============ SUPPRESSION ============
    public void deleteReview(String userEmail, Long reviewId) {
        ProductReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Avis introuvable"));

        if (review.getUser() == null || review.getUser().getEmail() == null ||
                !review.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("Vous ne pouvez supprimer que vos propres avis.");
        }

        reviewRepo.delete(review);
    }

    // ============ HELPERS ============

    private String buildAuthorName(User user) {
        String fn = user.getFirstName() != null ? user.getFirstName() : "";
        String ln = user.getLastName() != null ? user.getLastName() : "";
        String full = (fn + " " + ln).trim();
        return full.isEmpty() ? user.getEmail() : full;
    }

    private ProductReviewResponse toDto(ProductReview r, boolean mine) {
        return ProductReviewResponse.builder()
                .id(r.getId())
                .authorName(r.getAuthorName())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .mine(mine)
                .build();
    }
}
