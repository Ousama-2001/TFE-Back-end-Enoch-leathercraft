// src/main/java/com/enoch/leathercraft/repository/ProductReviewRepository.java
package com.enoch.leathercraft.repository;

import com.enoch.leathercraft.entities.ProductReview;
import com.enoch.leathercraft.entities.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    // Pour l'affichage public / produit
    List<ProductReview> findByProduct_IdOrderByCreatedAtDesc(Long productId);

    // Pour les écrans admin global
    List<ProductReview> findAllByOrderByCreatedAtDesc();

    List<ProductReview> findByStatusOrderByCreatedAtDesc(ReviewStatus status);
}
