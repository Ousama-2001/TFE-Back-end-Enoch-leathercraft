package com.enoch.leathercraft.repository;

import com.enoch.leathercraft.entities.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProduct_IdOrderByCreatedAtDesc(Long productId);
}
