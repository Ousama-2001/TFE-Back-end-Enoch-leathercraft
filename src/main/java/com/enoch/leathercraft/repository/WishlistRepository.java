// src/main/java/com/enoch/leathercraft/repositories/WishlistRepository.java
package com.enoch.leathercraft.repository;

import com.enoch.leathercraft.entities.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<WishlistItem, Long> {

    // pour vérifier si un produit est déjà dans la wishlist de l'user
    @Query("""
           select w from WishlistItem w 
           where w.user.id = :userId 
             and w.product.id = :productId
           """)
    Optional<WishlistItem> findByUserIdAndProductId(@Param("userId") Long userId,
                                                    @Param("productId") Long productId);

    // pour récupérer toute la wishlist d'un user
    List<WishlistItem> findByUserId(Long userId);

    // pour supprimer un produit de la wishlist
    @Modifying
    @Query("""
           delete from WishlistItem w 
           where w.user.id = :userId 
             and w.product.id = :productId
           """)
    void deleteByUserIdAndProductId(@Param("userId") Long userId,
                                    @Param("productId") Long productId);
}
