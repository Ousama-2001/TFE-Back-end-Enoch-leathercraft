// src/main/java/com/enoch/leathercraft/repository/ProductRepository.java
package com.enoch.leathercraft.repository;

import com.enoch.leathercraft.entities.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    @Override
    @EntityGraph(attributePaths = {"images"})
    List<Product> findAll();

    @EntityGraph(attributePaths = {"images"})
    List<Product> findByIsActiveTrueAndDeletedFalseOrderByNameAsc();

    @EntityGraph(attributePaths = {"images"})
    Optional<Product> findByIdAndDeletedFalse(Long id);

    @EntityGraph(attributePaths = {"images"})
    List<Product> findByDeletedFalse();

    @EntityGraph(attributePaths = {"images"})
    List<Product> findByDeletedTrueOrderByUpdatedAtDesc();

    // 🌟 Produits actifs filtrés par catégorie (slug)
    @EntityGraph(attributePaths = {"images"})
    @Query("""
           SELECT DISTINCT p
           FROM Product p, ProductCategory pc, Category c
           WHERE p.id = pc.productId
             AND c.id = pc.categoryId
             AND p.deleted = false
             AND (p.isActive = true OR p.isActive IS NULL)
             AND LOWER(c.slug) = LOWER(:slug)
           ORDER BY p.name ASC
           """)
    List<Product> findActiveByCategorySlug(@Param("slug") String slug);
}
