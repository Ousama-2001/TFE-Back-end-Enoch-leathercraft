package com.enoch.leathercraft.repository;

import com.enoch.leathercraft.entities.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductIdOrderByPositionAsc(Long productId);
}