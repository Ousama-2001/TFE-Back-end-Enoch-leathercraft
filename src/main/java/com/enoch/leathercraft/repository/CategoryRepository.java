package com.enoch.leathercraft.repository;

import com.enoch.leathercraft.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlugIgnoreCase(String slug);
}
