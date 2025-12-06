// src/main/java/com/enoch/leathercraft/repositories/WishlistItemRepository.java
package com.enoch.leathercraft.repository;

import com.enoch.leathercraft.entities.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// src/main/java/com/enoch/leathercraft/repositories/WishlistRepository.java
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    List<WishlistItem> findAllByUserId(Long userId);

    Optional<WishlistItem> findByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);

    void deleteAllByUserId(Long userId);
}

