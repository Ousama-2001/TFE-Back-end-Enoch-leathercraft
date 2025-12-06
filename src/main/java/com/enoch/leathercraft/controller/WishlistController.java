// src/main/java/com/enoch/leathercraft/controller/WishlistController.java
package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.auth.domain.User;
import com.enoch.leathercraft.auth.repo.UserRepository;
import com.enoch.leathercraft.dto.WishlistItemResponse;
import com.enoch.leathercraft.entities.WishlistItem;
import com.enoch.leathercraft.services.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserRepository userRepository;

    private User getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable pour l'email : " + email));
    }

    @GetMapping
    public List<WishlistItemResponse> getWishlist(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<WishlistItem> items = wishlistService.getWishlistForUser(user.getId());
        return items.stream()
                .map(WishlistItemResponse::fromEntity)
                .toList();
    }

    @PostMapping("/{productId}")
    public WishlistItemResponse toggleWishlist(Authentication authentication,
                                               @PathVariable Long productId) {
        User user = getCurrentUser(authentication);
        WishlistItem item = wishlistService.toggleProduct(user.getId(), productId);
        return WishlistItemResponse.fromEntity(item);
    }

    @DeleteMapping("/{productId}")
    public void removeFromWishlist(Authentication authentication,
                                   @PathVariable Long productId) {
        User user = getCurrentUser(authentication);
        wishlistService.removeFromWishlist(user.getId(), productId);
    }
}
