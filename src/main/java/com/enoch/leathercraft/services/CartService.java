package com.enoch.leathercraft.services;

import com.enoch.leathercraft.auth.domain.User;
import com.enoch.leathercraft.auth.repo.UserRepository;
import com.enoch.leathercraft.dto.CartAddRequest;
import com.enoch.leathercraft.dto.CartItemResponse;
import com.enoch.leathercraft.dto.CartResponse;
import com.enoch.leathercraft.dto.CartUpdateRequest;
import com.enoch.leathercraft.entities.Cart;
import com.enoch.leathercraft.entities.CartItem;
import com.enoch.leathercraft.entities.CartStatus;
import com.enoch.leathercraft.entities.Product;
import com.enoch.leathercraft.repository.CartItemRepository;
import com.enoch.leathercraft.repository.CartRepository;
import com.enoch.leathercraft.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // ---------- helpers ----------

    /**
     * TEMPORAIRE :
     * On utilise toujours le même utilisateur (par exemple ton admin)
     * pour le panier, pour éviter les problèmes de SecurityContext/JWT.
     */
    private User getFixedUser() {
        String email = "ousama850@gmail.com"; // 🔴 mets ici un email qui existe dans ta table users
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new EntityNotFoundException("Utilisateur fixe introuvable: " + email));
    }

    private Cart getOrCreateOpenCart(User user) {
        return cartRepository.findByUserAndStatus(user, CartStatus.OPEN)
                .orElseGet(() -> {
                    Cart c = new Cart();
                    c.setUser(user);
                    c.setStatus(CartStatus.OPEN);
                    c.setCreatedAt(Instant.now());
                    c.setUpdatedAt(Instant.now());
                    return cartRepository.save(c); // INSERT ici => doit être dans une transaction NON read-only
                });
    }

    private CartResponse toDto(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(ci -> CartItemResponse.builder()
                        .productId(ci.getProduct().getId())
                        .name(ci.getProduct().getName())
                        .sku(ci.getProduct().getSku())
                        .unitPrice(ci.getUnitPrice())
                        .quantity(ci.getQuantity())
                        .lineTotal(ci.getLineTotal())
                        .build()
                )
                .toList();

        int totalQty = items.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        BigDecimal totalAmount = items.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(items)
                .totalQuantity(totalQty)
                .totalAmount(totalAmount)
                .build();
    }

    // ---------- API métier ----------

    // ❌ Surtout PAS readOnly = true ici, on peut créer un panier
    public CartResponse getCurrentCart() {
        User user = getFixedUser();
        Cart cart = getOrCreateOpenCart(user);
        return toDto(cart);
    }

    public CartResponse addItem(CartAddRequest req) {
        if (req.getQuantity() == null || req.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantité invalide");
        }

        User user = getFixedUser();
        Cart cart = getOrCreateOpenCart(user);

        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

        CartItem item = cart.getItems().stream()
                .filter(ci -> ci.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(req.getQuantity());
            item.setUnitPrice(product.getPrice());
            item.setLineTotal(
                    product.getPrice().multiply(BigDecimal.valueOf(req.getQuantity()))
            );
            cart.getItems().add(item);
        } else {
            int newQty = item.getQuantity() + req.getQuantity();
            item.setQuantity(newQty);
            item.setLineTotal(
                    item.getUnitPrice().multiply(BigDecimal.valueOf(newQty))
            );
        }

        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        return toDto(cart);
    }

    public CartResponse updateItem(Long productId, CartUpdateRequest req) {
        User user = getFixedUser();
        Cart cart = getOrCreateOpenCart(user);

        CartItem item = cart.getItems().stream()
                .filter(ci -> ci.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Ligne de panier introuvable"));

        int newQty = req.getQuantity() == null ? 0 : req.getQuantity();

        if (newQty <= 0) {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(newQty);
            item.setLineTotal(
                    item.getUnitPrice().multiply(BigDecimal.valueOf(newQty))
            );
        }

        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        return toDto(cart);
    }

    public CartResponse removeItem(Long productId) {
        CartUpdateRequest req = new CartUpdateRequest();
        req.setQuantity(0);
        return updateItem(productId, req);
    }

    public CartResponse clearCart() {
        User user = getFixedUser();
        Cart cart = getOrCreateOpenCart(user);

        cart.getItems().clear();
        cartItemRepository.deleteAll(cartItemRepository.findAll()); // à optimiser plus tard
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        return toDto(cart);
    }
}
