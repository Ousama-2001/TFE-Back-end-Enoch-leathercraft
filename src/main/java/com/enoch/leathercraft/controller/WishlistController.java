// src/main/java/com/enoch/leathercraft/controller/WishlistController.java
package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.dto.WishlistItemResponse;
import com.enoch.leathercraft.entities.WishlistItem;
import com.enoch.leathercraft.services.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public List<WishlistItemResponse> getWishlist(Authentication authentication) {
        List<WishlistItem> items = wishlistService.getWishlist(authentication);
        return items.stream()
                .map(WishlistItemResponse::fromEntity)
                .toList();
    }

    @PostMapping("/{productId}")
    public WishlistItemResponse toggleWishlist(Authentication authentication,
                                               @PathVariable Long productId) {
        WishlistItem item = wishlistService.toggleWishlist(authentication, productId);
        // Si toggle = "unlike", le service peut retourner null → côté front on se base sur la liste mise à jour
        return (item != null) ? WishlistItemResponse.fromEntity(item) : null;
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromWishlist(Authentication authentication,
                                   @PathVariable Long productId) {
        wishlistService.removeFromWishlist(authentication, productId);
    }

    @DeleteMapping("/clear")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearWishlist(Authentication authentication) {
        wishlistService.clearWishlist(authentication);
    }
}
