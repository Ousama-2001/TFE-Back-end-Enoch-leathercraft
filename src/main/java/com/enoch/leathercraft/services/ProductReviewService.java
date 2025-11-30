// src/main/java/com/enoch/leathercraft/services/ProductReviewService.java
package com.enoch.leathercraft.services;

import com.enoch.leathercraft.auth.domain.User;
import com.enoch.leathercraft.auth.repo.UserRepository;
import com.enoch.leathercraft.dto.AdminReviewResponse;
import com.enoch.leathercraft.dto.ProductReviewCreateRequest;
import com.enoch.leathercraft.dto.ProductReviewResponse;
import com.enoch.leathercraft.entities.Product;
import com.enoch.leathercraft.entities.ProductReview;
import com.enoch.leathercraft.entities.ReviewStatus;
import com.enoch.leathercraft.repository.ProductRepository;
import com.enoch.leathercraft.repository.ProductReviewRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductReviewService {

    private final ProductReviewRepository reviewRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    // =========================================================
    //                 MÉTHODES UTILISATEUR
    // =========================================================

    // CRÉATION D'UN AVIS PAR UN USER CONNECTÉ
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
                .status(ReviewStatus.VISIBLE)   // avis directement visible
                .build();

        review = reviewRepo.save(review);
        return toDto(review, true);
    }

    // LECTURE POUR UNE PAGE PRODUIT
    @Transactional(readOnly = true)
    public List<ProductReviewResponse> getReviewsForProduct(Long productId, String currentUserEmail) {
        return reviewRepo.findByProduct_IdOrderByCreatedAtDesc(productId)
                .stream()
                // on masque les DELETED pour tout le monde
                .filter(r -> r.getStatus() != ReviewStatus.DELETED)
                .map(r -> {
                    boolean mine = false;
                    if (currentUserEmail != null
                            && r.getUser() != null
                            && r.getUser().getEmail() != null) {
                        mine = currentUserEmail.equalsIgnoreCase(r.getUser().getEmail());
                    }
                    return toDto(r, mine);
                })
                .toList();
    }

    // MISE A JOUR PAR L’AUTEUR UNIQUEMENT
    public ProductReviewResponse updateReview(String userEmail,
                                              Long reviewId,
                                              ProductReviewCreateRequest req) {

        ProductReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Avis introuvable"));

        checkIsOwner(review, userEmail);

        review.setRating(req.getRating());
        review.setComment(req.getComment());

        review = reviewRepo.save(review);
        return toDto(review, true);
    }

    // SUPPRESSION PAR L’AUTEUR (SOFT DELETE)
    public void deleteReview(String userEmail, Long reviewId) {
        ProductReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Avis introuvable"));

        checkIsOwner(review, userEmail);

        review.setStatus(ReviewStatus.DELETED);
        review.setDeletedAt(Instant.now());
        reviewRepo.save(review);
    }

    // =========================================================
    //                 MÉTHODES ADMIN (ADMIN + SUPER_ADMIN)
    // =========================================================

    @Transactional(readOnly = true)
    public List<AdminReviewResponse> adminGetAll(ReviewStatus status) {
        List<ProductReview> reviews;
        if (status != null) {
            reviews = reviewRepo.findByStatusOrderByCreatedAtDesc(status);
        } else {
            reviews = reviewRepo.findAllByOrderByCreatedAtDesc();
        }
        return reviews.stream().map(this::toAdminDto).toList();
    }

    public AdminReviewResponse adminChangeStatus(Long reviewId, ReviewStatus newStatus) {
        ProductReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Avis introuvable"));

        review.setStatus(newStatus);
        if (newStatus == ReviewStatus.DELETED) {
            review.setDeletedAt(Instant.now());
        } else {
            review.setDeletedAt(null);
        }

        review = reviewRepo.save(review);
        return toAdminDto(review);
    }

    // =========================================================
    //                 MÉTHODES SUPER ADMIN
    // =========================================================

    @Transactional(readOnly = true)
    public List<AdminReviewResponse> superAdminGetDeleted() {
        return reviewRepo.findByStatusOrderByCreatedAtDesc(ReviewStatus.DELETED)
                .stream()
                .map(this::toAdminDto)
                .toList();
    }

    // Hard delete DEFINITIF
    public void superAdminHardDelete(Long reviewId) {
        // On vérifie juste, mais on ne jette plus une exception si ça n'existe pas
        if (reviewRepo.existsById(reviewId)) {
            reviewRepo.deleteById(reviewId);
        }
        // Si ça n'existe pas, on ne fait rien → côté API on renverra 204 quand même
    }


    // Restaurer un avis soft-supprimé
    public AdminReviewResponse superAdminRestore(Long reviewId) {
        ProductReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Avis introuvable"));

        review.setStatus(ReviewStatus.VISIBLE);
        review.setDeletedAt(null);

        review = reviewRepo.save(review);
        return toAdminDto(review);
    }

    // =========================================================
    //                 HELPERS
    // =========================================================

    private String buildAuthorName(User user) {
        String fn = user.getFirstName() != null ? user.getFirstName() : "";
        String ln = user.getLastName() != null ? user.getLastName() : "";
        String full = (fn + " " + ln).trim();
        return full.isEmpty() ? user.getEmail() : full;
    }

    private void checkIsOwner(ProductReview review, String email) {
        if (review.getUser() == null
                || review.getUser().getEmail() == null
                || !review.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("Vous ne pouvez modifier/supprimer que vos propres avis.");
        }
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

    private AdminReviewResponse toAdminDto(ProductReview r) {
        return AdminReviewResponse.builder()
                .id(r.getId())
                .productId(r.getProduct() != null ? r.getProduct().getId() : null)
                .productName(r.getProduct() != null ? r.getProduct().getName() : null)
                .userId(r.getUser() != null ? r.getUser().getId() : null)
                .userEmail(r.getUser() != null ? r.getUser().getEmail() : null)
                .authorName(r.getAuthorName())
                .rating(r.getRating())
                .comment(r.getComment())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .deletedAt(r.getDeletedAt())
                .build();
    }
}
