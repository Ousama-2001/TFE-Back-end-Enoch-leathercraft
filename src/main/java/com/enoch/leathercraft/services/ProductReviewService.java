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
                .status(ReviewStatus.VISIBLE) // Par défaut : Visible
                .build();

        review = reviewRepo.save(review);
        return toDto(review, true);
    }

    @Transactional(readOnly = true)
    public List<ProductReviewResponse> getReviewsForProduct(Long productId, String currentUserEmail) {
        return reviewRepo.findByProduct_IdOrderByCreatedAtDesc(productId)
                .stream()
                .filter(r -> shouldShowReview(r, currentUserEmail)) // ✅ Filtre : Visible OU (Supprimé ET à moi)
                .map(r -> {
                    boolean mine = isMine(r, currentUserEmail);
                    return toDto(r, mine);
                })
                .toList();
    }

    /**
     * LOGIQUE D'AFFICHAGE :
     * 1. Si VISIBLE -> Tout le monde voit.
     * 2. Si DELETED -> Seul l'auteur voit (pour voir l'alerte "Supprimé").
     */
    private boolean shouldShowReview(ProductReview r, String currentUserEmail) {
        if (r.getStatus() == ReviewStatus.VISIBLE) {
            return true;
        }
        // Si DELETED, on ne montre que si c'est l'auteur
        return isMine(r, currentUserEmail);
    }

    private boolean isMine(ProductReview r, String currentUserEmail) {
        return currentUserEmail != null
                && r.getUser() != null
                && r.getUser().getEmail() != null
                && r.getUser().getEmail().equalsIgnoreCase(currentUserEmail);
    }

    public ProductReviewResponse updateReview(String userEmail, Long reviewId, ProductReviewCreateRequest req) {
        ProductReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Avis introuvable"));

        checkIsOwner(review, userEmail);

        review.setRating(req.getRating());
        review.setComment(req.getComment());

        review = reviewRepo.save(review);
        return toDto(review, true);
    }

    public void deleteReview(String userEmail, Long reviewId) {
        ProductReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Avis introuvable"));

        checkIsOwner(review, userEmail);

        // Soft Delete
        review.setStatus(ReviewStatus.DELETED);
        review.setDeletedAt(Instant.now());
        reviewRepo.save(review);
    }

    // =========================================================
    //                 MÉTHODES ADMIN
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

    // Admin : Basculer entre VISIBLE et DELETED (Soft Delete / Restauration)
    public AdminReviewResponse adminChangeStatus(Long reviewId, ReviewStatus newStatus) {
        ProductReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Avis introuvable"));

        review.setStatus(newStatus);

        if (newStatus == ReviewStatus.DELETED) {
            review.setDeletedAt(Instant.now());
        } else {
            review.setDeletedAt(null); // Restauration
        }

        review = reviewRepo.save(review);
        return toAdminDto(review);
    }

    // =========================================================
    //        ✅ MÉTHODES SUPER ADMIN (OBLIGATOIRES POUR LE CONTROLLER)
    // =========================================================

    // 1. Récupérer la corbeille (Soft Deleted)
    @Transactional(readOnly = true)
    public List<AdminReviewResponse> superAdminGetDeleted() {
        return reviewRepo.findByStatusOrderByCreatedAtDesc(ReviewStatus.DELETED)
                .stream()
                .map(this::toAdminDto)
                .toList();
    }

    // 2. Restaurer (Alias vers la méthode admin)
    public AdminReviewResponse superAdminRestore(Long reviewId) {
        return adminChangeStatus(reviewId, ReviewStatus.VISIBLE);
    }

    // 3. Suppression DÉFINITIVE (Hard Delete)
    public void superAdminHardDelete(Long reviewId) {
        if (reviewRepo.existsById(reviewId)) {
            reviewRepo.deleteById(reviewId);
        }
    }

    // =========================================================
    //                 HELPERS & DTOs
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
            throw new AccessDeniedException("Non autorisé.");
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
                .status(r.getStatus()) // Nécessaire pour l'affichage conditionnel front
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