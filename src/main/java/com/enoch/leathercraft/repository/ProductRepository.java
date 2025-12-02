// src/main/java/com/enoch/leathercraft/repository/ProductRepository.java
package com.enoch.leathercraft.repository;

import com.enoch.leathercraft.entities.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    @Override
    @EntityGraph(attributePaths = {"images"})
    List<Product> findAll();

    // ✅ Un produit non supprimé par id
    @EntityGraph(attributePaths = {"images"})
    Optional<Product> findByIdAndDeletedFalse(Long id);

    // ✅ Tous les produits non supprimés
    @EntityGraph(attributePaths = {"images"})
    List<Product> findByDeletedFalse();

    // ✅ Produits visibles dans le catalogue
    @EntityGraph(attributePaths = {"images"})
    List<Product> findByIsActiveTrueAndDeletedFalseOrderByNameAsc();
}
