package com.enoch.leathercraft.repository;

import com.enoch.leathercraft.entities.Product;
import org.springframework.data.jpa.repository.EntityGraph; // <-- NOUVEAU
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySlug(String slug);

    // MODIFICATION : Utilisation de EntityGraph pour charger la relation 'images'
    // afin d'éviter la LazyInitializationException
    @Override
    @EntityGraph(attributePaths = {"images"})
    List<Product> findAll();
}