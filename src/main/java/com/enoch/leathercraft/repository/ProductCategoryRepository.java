// src/main/java/com/enoch/leathercraft/repository/ProductCategoryRepository.java
package com.enoch.leathercraft.repository;

import com.enoch.leathercraft.entities.ProductCategory;
import com.enoch.leathercraft.entities.ProductCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductCategoryRepository
        extends JpaRepository<ProductCategory, ProductCategoryId> {

    List<ProductCategory> findByProductId(Long productId);

    void deleteByProductId(Long productId);
}
