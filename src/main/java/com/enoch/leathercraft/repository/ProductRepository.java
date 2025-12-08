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

    // Charge toujours les images
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

    // 🌟 Produits actifs filtrés par une seule catégorie (slug)
    @EntityGraph(attributePaths = {"images"})
    @Query("""
           SELECT DISTINCT p
           FROM Product p
           JOIN ProductCategory pc ON pc.productId = p.id
           JOIN Category c ON c.id = pc.categoryId
           WHERE p.deleted = false
             AND (p.isActive = true OR p.isActive IS NULL)
             AND LOWER(c.slug) = LOWER(:slug)
           ORDER BY p.name ASC
           """)
    List<Product> findActiveByCategorySlug(@Param("slug") String slug);

    // 🌟 Produits actifs filtrés par "segment" (slug) → Homme, Femme, etc.
    @EntityGraph(attributePaths = {"images"})
    @Query("""
           SELECT DISTINCT p
           FROM Product p
           JOIN ProductCategory pcSeg ON pcSeg.productId = p.id
           JOIN Category cSeg ON cSeg.id = pcSeg.categoryId
           WHERE p.deleted = false
             AND (p.isActive = true OR p.isActive IS NULL)
             AND LOWER(cSeg.slug) = LOWER(:segmentSlug)
           ORDER BY p.name ASC
           """)
    List<Product> findActiveBySegmentSlug(@Param("segmentSlug") String segmentSlug);

    // 🌟 Produits actifs filtrés par segment + type
    @EntityGraph(attributePaths = {"images"})
    @Query("""
           SELECT DISTINCT p
           FROM Product p
           JOIN ProductCategory pcSeg ON pcSeg.productId = p.id
           JOIN Category cSeg ON cSeg.id = pcSeg.categoryId
           JOIN ProductCategory pcCat ON pcCat.productId = p.id
           JOIN Category cCat ON cCat.id = pcCat.categoryId
           WHERE p.deleted = false
             AND (p.isActive = true OR p.isActive IS NULL)
             AND LOWER(cSeg.slug) = LOWER(:segmentSlug)
             AND LOWER(cCat.slug) = LOWER(:categorySlug)
           ORDER BY p.name ASC
           """)
    List<Product> findActiveBySegmentAndCategory(@Param("segmentSlug") String segmentSlug,
                                                 @Param("categorySlug") String categorySlug);
}
