// src/main/java/com/enoch/leathercraft/services/WishlistService.java
package com.enoch.leathercraft.services;

import com.enoch.leathercraft.auth.domain.User;
import com.enoch.leathercraft.auth.repo.UserRepository;
import com.enoch.leathercraft.entities.Product;
import com.enoch.leathercraft.entities.WishlistItem;
import com.enoch.leathercraft.repository.ProductRepository;

import com.enoch.leathercraft.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    // 🔹 Récupère l'user connecté à partir du token JWT (email dans subject)
    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName(); // = subject du token
        return userRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable"));
    }

    @Transactional(readOnly = true)
    public List<WishlistItem> getWishlist(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return wishlistRepository.findByUserId(user.getId()); // ✅ user.getId OK ici
    }

    @Transactional
    public void addToWishlist(Authentication authentication, Long productId) {
        User user = getCurrentUser(authentication);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Produit introuvable : " + productId));

        // déjà présent ? on ne duplique pas
        boolean exists = wishlistRepository
                .findByUserIdAndProductId(user.getId(), productId) // ✅ user.getId OK
                .isPresent();

        if (exists) {
            return; // rien à faire
        }

        WishlistItem item = new WishlistItem();
        item.setUser(user);
        item.setProduct(product);
        wishlistRepository.save(item);
    }

    @Transactional
    public void removeFromWishlist(Authentication authentication, Long productId) {
        User user = getCurrentUser(authentication);
        wishlistRepository.deleteByUserIdAndProductId(user.getId(), productId); // ✅ user.getId OK
    }
}
