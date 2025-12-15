// src/main/java/com/enoch/leathercraft/repository/ProductImageRepository.java
package com.enoch.leathercraft.repository;

import com.enoch.leathercraft.entities.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    // ✅ OK : product (relation) -> id (champ de Product)
    List<ProductImage> findByProductIdOrderByPositionAsc(Long productId);

    // ✅ utile aussi si tu veux tout delete vite
    void deleteByProductId(Long productId);
}
