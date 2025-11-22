package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.dto.CartAddRequest;
import com.enoch.leathercraft.dto.CartResponse;
import com.enoch.leathercraft.dto.CartUpdateRequest;
import com.enoch.leathercraft.services.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // GET /api/cart
    @GetMapping
    public ResponseEntity<CartResponse> getCurrentCart() {
        return ResponseEntity.ok(cartService.getCurrentCart());
    }

    // POST /api/cart/items
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@RequestBody CartAddRequest req) {
        return ResponseEntity.ok(cartService.addItem(req));
    }

    // PATCH /api/cart/items/{productId}
    @PatchMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateItem(
            @PathVariable Long productId,
            @RequestBody CartUpdateRequest req
    ) {
        return ResponseEntity.ok(cartService.updateItem(productId, req));
    }

    // DELETE /api/cart/items/{productId}
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(@PathVariable Long productId) {
        return ResponseEntity.ok(cartService.removeItem(productId));
    }

    // DELETE /api/cart
    @DeleteMapping
    public ResponseEntity<CartResponse> clearCart() {
        return ResponseEntity.ok(cartService.clearCart());
    }
    // GET /api/cart/debug
    @GetMapping("/debug")
    public ResponseEntity<String> debugCart() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String name = (authentication != null ? authentication.getName() : "null");
        return ResponseEntity.ok("SecurityContext principal = " + name);
    }


}
