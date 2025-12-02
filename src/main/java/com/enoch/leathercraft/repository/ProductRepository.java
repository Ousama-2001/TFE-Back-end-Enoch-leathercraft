// src/main/java/com/enoch/leathercraft/repository/ProductRepository.java
package com.enoch.leathercraft.repository;

import com.enoch.leathercraft.entities.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    // Charge toujours les images avec les produits
    @Override
    @EntityGraph(attributePaths = {"images"})
    List<Product> findAll();

    // 🌟 Pour le catalogue public / admin "classique"
    @EntityGraph(attributePaths = {"images"})
    List<Product> findByIsActiveTrueAndDeletedFalseOrderByNameAsc();

    @EntityGraph(attributePaths = {"images"})
    Optional<Product> findByIdAndDeletedFalse(Long id);

    @EntityGraph(attributePaths = {"images"})
    List<Product> findByDeletedFalse();

    // 🌟 Pour l'écran "Produits archivés"
    @EntityGraph(attributePaths = {"images"})
    List<Product> findByDeletedTrueOrderByUpdatedAtDesc();
}
