// src/main/java/com/enoch/leathercraft/services/WishlistService.java
package com.enoch.leathercraft.services;

import com.enoch.leathercraft.auth.domain.User;
import com.enoch.leathercraft.auth.repo.UserRepository;
import com.enoch.leathercraft.entities.Product;
import com.enoch.leathercraft.entities.WishlistItem;
import com.enoch.leathercraft.repository.ProductRepository;
import com.enoch.leathercraft.repository.WishlistItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /**
     * Retourne tous les items wishlist pour un utilisateur.
     */
    public List<WishlistItem> getWishlistForUser(Long userId) {
        return wishlistItemRepository.findByUserId(userId);
    }

    /**
     * Toggle : si le produit est déjà dans la wishlist -> on le retire,
     * sinon on le crée.
     */
    @Transactional
    public WishlistItem toggleProduct(Long userId, Long productId) {
        Optional<WishlistItem> existingOpt =
                wishlistItemRepository.findByUserIdAndProductId(userId, productId);

        if (existingOpt.isPresent()) {
            WishlistItem existing = existingOpt.get();
            wishlistItemRepository.delete(existing);
            return existing; // on renvoie l’item supprimé (le front ne s'en sert pas vraiment)
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

        WishlistItem item = new WishlistItem();
        item.setUser(user);
        item.setProduct(product);
        item.setCreatedAt(Instant.now());

        return wishlistItemRepository.save(item);
    }

    /**
     * Supprime un produit de la wishlist pour un user.
     */
    @Transactional
    public void removeFromWishlist(Long userId, Long productId) {
        wishlistItemRepository.deleteByUserIdAndProductId(userId, productId);
    }
}
