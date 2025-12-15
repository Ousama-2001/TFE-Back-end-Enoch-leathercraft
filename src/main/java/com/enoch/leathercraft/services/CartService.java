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
import com.enoch.leathercraft.repository.CartRepository;
import com.enoch.leathercraft.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    private static final Duration CART_TTL = Duration.ofMinutes(15);

    // =========================
    // PUBLIC API
    // =========================

    @Transactional
    public CartResponse getCurrentCart() {
        User user = getAuthenticatedUser();
        Cart cart = getOrCreateCart(user);

        clearIfExpired(cart);

        // on recharge au cas où clearIfExpired a sauvegardé
        cart = getOrCreateCart(user);
        return toDto(cart);
    }

    @Transactional
    public CartResponse addItem(CartAddRequest req) {
        User user = getAuthenticatedUser();
        Cart cart = getOrCreateCart(user);

        clearIfExpired(cart);

        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst();

        int qtyToAdd = (req.getQuantity() == null || req.getQuantity() < 1) ? 1 : req.getQuantity();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + qtyToAdd;

            item.setQuantity(newQuantity);
            item.setUnitPrice(product.getPrice());
            item.setLineTotal(product.getPrice().multiply(BigDecimal.valueOf(newQuantity)));

        } else {
            BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(qtyToAdd));

            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(qtyToAdd)
                    .unitPrice(product.getPrice())
                    .lineTotal(total)
                    .build();

            cart.getItems().add(newItem);
        }

        refreshExpiry(cart);
        cartRepository.save(cart);

        return toDto(cart);
    }

    @Transactional
    public CartResponse updateItem(Long productId, CartUpdateRequest req) {
        User user = getAuthenticatedUser();
        Cart cart = getOrCreateCart(user);

        clearIfExpired(cart);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Article non trouvé"));

        int newQty = (req.getQuantity() == null) ? item.getQuantity() : req.getQuantity();

        // 0 ou moins => suppression
        if (newQty <= 0) {
            cart.getItems().remove(item);

            if (cart.getItems().isEmpty()) {
                cart.setExpiresAt(null);
                cart.setUpdatedAt(Instant.now());
            } else {
                refreshExpiry(cart);
            }

            cartRepository.save(cart);
            return toDto(cart);
        }

        item.setQuantity(newQty);

        // prix actuel produit (si tu veux figer, mets item.getUnitPrice())
        BigDecimal unit = item.getProduct().getPrice();
        item.setUnitPrice(unit);
        item.setLineTotal(unit.multiply(BigDecimal.valueOf(newQty)));

        refreshExpiry(cart);
        cartRepository.save(cart);

        return toDto(cart);
    }

    @Transactional
    public CartResponse removeItem(Long productId) {
        User user = getAuthenticatedUser();
        Cart cart = getOrCreateCart(user);

        clearIfExpired(cart);

        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));

        if (cart.getItems().isEmpty()) {
            cart.setExpiresAt(null);
            cart.setUpdatedAt(Instant.now());
        } else {
            refreshExpiry(cart);
        }

        cartRepository.save(cart);
        return toDto(cart);
    }

    @Transactional
    public CartResponse clearCartByCurrentUser() {
        User user = getAuthenticatedUser();
        clearCart(user.getEmail());
        return getCurrentCart();
    }

    @Transactional
    public void clearCart(String userEmail) {
        Cart cart = cartRepository.findByUser_EmailAndStatus(userEmail, CartStatus.OPEN)
                .orElseThrow(() -> new EntityNotFoundException("Panier introuvable"));

        cart.getItems().clear();
        cart.setExpiresAt(null);
        cart.setUpdatedAt(Instant.now());

        cartRepository.save(cart);
    }

    // =========================
    // EXPIRATION HELPERS (15 min)
    // =========================

    private void refreshExpiry(Cart cart) {
        cart.setExpiresAt(Instant.now().plus(CART_TTL));
        cart.setUpdatedAt(Instant.now());
    }

    private boolean isExpired(Cart cart) {
        return cart.getExpiresAt() != null && Instant.now().isAfter(cart.getExpiresAt());
    }

    private void clearIfExpired(Cart cart) {
        if (!isExpired(cart)) return;

        cart.getItems().clear();
        cart.setExpiresAt(null);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
    }

    // =========================
    // PRIVATE HELPERS
    // =========================

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserAndStatus(user, CartStatus.OPEN)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    newCart.setStatus(CartStatus.OPEN);
                    newCart.setItems(new ArrayList<>());
                    newCart.setCreatedAt(Instant.now());
                    newCart.setUpdatedAt(Instant.now());
                    newCart.setExpiresAt(null); // pas de timer tant que panier vide
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse toDto(Cart cart) {
        double totalAmt = cart.getItems().stream()
                .mapToDouble(i -> {
                    if (i.getLineTotal() != null) return i.getLineTotal().doubleValue();
                    return i.getProduct().getPrice().doubleValue() * i.getQuantity();
                })
                .sum();

        int totalQty = cart.getItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        var itemsDto = cart.getItems().stream().map(item -> {
            String imgUrl = null;
            if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
                imgUrl = item.getProduct().getImages().iterator().next().getUrl();
            }

            return CartItemResponse.builder()
                    .productId(item.getProduct().getId())
                    .name(item.getProduct().getName())
                    .sku(item.getProduct().getSku())
                    .unitPrice(item.getUnitPrice() != null ? item.getUnitPrice() : item.getProduct().getPrice())
                    .quantity(item.getQuantity())
                    .lineTotal(item.getLineTotal())
                    .imageUrl(imgUrl)
                    .stockQuantity(item.getProduct().getStockQuantity()) // ✅ AJOUT
                    .build();
        }).collect(Collectors.toList());

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(itemsDto)
                .totalQuantity(totalQty)
                .totalAmount(BigDecimal.valueOf(totalAmt))
                .expiresAt(cart.getExpiresAt()) // ✅ nécessite le champ dans CartResponse
                .build();
    }
}
