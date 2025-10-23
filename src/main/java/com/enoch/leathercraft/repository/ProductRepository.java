// ProductRepository.java
package com.enoch.leathercraft.repository;

import com.enoch.leathercraft.entities.Product;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByNameContainingIgnoreCase(String q, Pageable pageable);
}
