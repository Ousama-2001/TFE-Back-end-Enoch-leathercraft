// src/main/java/com/enoch/leathercraft/controller/WishlistController.java
package com.enoch.leathercraft.controller;

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

    @GetMapping
    public List<WishlistItem> getWishlist(Authentication authentication) {
        return wishlistService.getWishlist(authentication);
    }

    @PostMapping("/{productId}")
    public void addToWishlist(Authentication authentication,
                              @PathVariable Long productId) {
        wishlistService.addToWishlist(authentication, productId);
    }

    @DeleteMapping("/{productId}")
    public void removeFromWishlist(Authentication authentication,
                                   @PathVariable Long productId) {
        wishlistService.removeFromWishlist(authentication, productId);
    }
}
