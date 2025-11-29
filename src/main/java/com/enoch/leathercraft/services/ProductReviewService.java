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

    // ================== AJOUTER UN AVIS ==================
    public ProductReviewResponse addReview(String userEmail, ProductReviewCreateRequest req) {

        Product product = productRepo.findById(req.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));

        // Construction d'un nom d'affichage propre
        String authorName = buildAuthorName(user);

        ProductReview review = ProductReview.builder()
                .product(product)
                .user(user)
                .authorName(authorName)
                .rating(req.getRating())
                .comment(req.getComment())
                .build();

        review = reviewRepo.save(review);
        return toDto(review);
    }

    // ================== LISTER LES AVIS D'UN PRODUIT ==================
    @Transactional(readOnly = true)
    public List<ProductReviewResponse> getReviewsForProduct(Long productId) {
        return reviewRepo.findByProduct_IdOrderByCreatedAtDesc(productId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // ================== HELPERS ==================

    private String buildAuthorName(User user) {
        String first = user.getFirstName();
        String last = user.getLastName();

        if ((first != null && !first.isBlank()) || (last != null && !last.isBlank())) {
            StringBuilder sb = new StringBuilder();
            if (first != null && !first.isBlank()) {
                sb.append(first.trim());
            }
            if (last != null && !last.isBlank()) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(last.trim());
            }
            return sb.toString();
        }

        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }

        return user.getEmail();
    }

    private ProductReviewResponse toDto(ProductReview r) {
        return ProductReviewResponse.builder()
                .id(r.getId())
                .authorName(r.getAuthorName())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
