// src/main/java/com/enoch/leathercraft/services/WishlistService.java
package com.enoch.leathercraft.services;

import com.enoch.leathercraft.auth.domain.User;
import com.enoch.leathercraft.auth.repo.UserRepository;
import com.enoch.leathercraft.entities.Product;
import com.enoch.leathercraft.entities.WishlistItem;

import com.enoch.leathercraft.repository.ProductRepository;
import com.enoch.leathercraft.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistItemRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /* ---------- HELPERS ---------- */

    /**
     * Dans ton JwtAuthFilter tu fais :
     *  new UsernamePasswordAuthenticationToken(email, null, authorities)
     * Donc ici le principal / getName() = email (String).
     */
    private User getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }

        // email = sub du JWT
        String email = authentication.getName(); // équivalent à (String) authentication.getPrincipal()

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable : " + email));
    }

    /* ---------- LECTURE ---------- */

    @Transactional(readOnly = true)
    public List<WishlistItem> getWishlist(Authentication authentication) {
        User user = getCurrentUser(authentication);
        // à adapter à ton repo : findByUserId, findAllByUser, etc.
        return wishlistRepository.findAllByUserId(user.getId());
    }

    /* ---------- TOGGLE (like / unlike) ---------- */

    @Transactional
    public WishlistItem toggleWishlist(Authentication authentication, Long productId) {
        User user = getCurrentUser(authentication);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Produit introuvable : " + productId));

        WishlistItem existing = wishlistRepository
                .findByUserIdAndProductId(user.getId(), productId)
                .orElse(null);

        if (existing != null) {
            wishlistRepository.delete(existing);
            return null; // côté front on saura que c’est un "unlike"
        }

        WishlistItem item = new WishlistItem();
        item.setUser(user);
        item.setProduct(product);
        item.setCreatedAt(Instant.now());

        return wishlistRepository.save(item);
    }

    /* ---------- SUPPRESSION D’UN SEUL PRODUIT ---------- */

    @Transactional
    public void removeFromWishlist(Authentication authentication, Long productId) {
        User user = getCurrentUser(authentication);
        wishlistRepository.deleteByUserIdAndProductId(user.getId(), productId);
    }

    /* ---------- VIDER TOUTE LA WISHLIST ---------- */

    @Transactional
    public void clearWishlist(Authentication authentication) {
        User user = getCurrentUser(authentication);
        wishlistRepository.deleteAllByUserId(user.getId());
    }
}
