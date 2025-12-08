// src/main/java/com/enoch/leathercraft/repository/CategoryRepository.java
package com.enoch.leathercraft.repository;

import com.enoch.leathercraft.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    List<Category> findByActiveTrueOrderByDisplayOrderAsc();
}
