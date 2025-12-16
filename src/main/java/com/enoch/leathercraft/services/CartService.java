package com.enoch.leathercraft.services;

import com.enoch.leathercraft.auth.domain.User;
import com.enoch.leathercraft.auth.repo.UserRepository;
import com.enoch.leathercraft.dto.CartAddRequest;
import com.enoch.leathercraft.dto.CartItemResponse;
import com.enoch.leathercraft.dto.CartResponse;
import com.enoch.leathercraft.dto.CartUpdateRequest;
import com.enoch.leathercraft.dto.CouponValidateResponse;
import com.enoch.leathercraft.entities.*;
import com.enoch.leathercraft.repository.CartRepository;
import com.enoch.leathercraft.repository.CouponRepository;
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
    private final CouponRepository couponRepository; // ✅ Nécessaire pour valider/sauvegarder

    private static final Duration CART_TTL = Duration.ofMinutes(30);

    // =========================
    // ✅ PROMO helpers
    // =========================
    private boolean isOnSale(Product p, Instant now) {
        if (p.getPromoPrice() == null) return false;
        if (p.getPromoStartAt() != null && now.isBefore(p.getPromoStartAt())) return false;
        if (p.getPromoEndAt() != null && now.isAfter(p.getPromoEndAt())) return false;
        return true;
    }

    private BigDecimal effectivePrice(Product p) {
        Instant now = Instant.now();
        return isOnSale(p, now) ? p.getPromoPrice() : p.getPrice();
    }

    // =========================
    // PUBLIC API
    // =========================

    @Transactional
    public CartResponse getCurrentCart() {
        User user = getAuthenticatedUser();
        Cart cart = getOrCreateCart(user);
        clearIfExpired(cart);

        // Nettoyage si vide
        if(cart.getItems().isEmpty() && cart.getCouponCode() != null) {
            clearCoupon(cart);
        }

        return toDto(cart);
    }

    @Transactional
    public CartResponse addItem(CartAddRequest req) {
        User user = getAuthenticatedUser();
        Cart cart = getOrCreateCart(user);
        clearIfExpired(cart);

        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

        int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        if (stock <= 0) throw new IllegalStateException("Produit en rupture de stock");

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst();

        int qtyToAdd = (req.getQuantity() == null || req.getQuantity() < 1) ? 1 : req.getQuantity();
        BigDecimal unit = effectivePrice(product);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + qtyToAdd;
            if (newQuantity > stock) throw new IllegalStateException("Stock insuffisant (max " + stock + ")");

            item.setQuantity(newQuantity);
            item.setUnitPrice(unit);
            item.setLineTotal(unit.multiply(BigDecimal.valueOf(newQuantity)));
        } else {
            if (qtyToAdd > stock) throw new IllegalStateException("Stock insuffisant (max " + stock + ")");

            BigDecimal total = unit.multiply(BigDecimal.valueOf(qtyToAdd));
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(qtyToAdd)
                    .unitPrice(unit)
                    .lineTotal(total)
                    .build();
            cart.getItems().add(newItem);
        }

        refreshExpiry(cart);
        return toDto(cartRepository.save(cart));
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

        if (newQty <= 0) {
            cart.getItems().remove(item);
        } else {
            Product product = item.getProduct();
            int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            if (newQty > stock) throw new IllegalStateException("Stock insuffisant (max " + stock + ")");

            item.setQuantity(newQty);
            BigDecimal unit = effectivePrice(product);
            item.setUnitPrice(unit);
            item.setLineTotal(unit.multiply(BigDecimal.valueOf(newQty)));
        }

        // ✅ Si panier vide -> suppression coupon
        if (cart.getItems().isEmpty()) {
            clearCoupon(cart);
            cart.setExpiresAt(null);
        } else {
            refreshExpiry(cart);
        }

        return toDto(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse removeItem(Long productId) {
        User user = getAuthenticatedUser();
        Cart cart = getOrCreateCart(user);
        clearIfExpired(cart);

        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));

        // ✅ Si panier vide -> suppression coupon
        if (cart.getItems().isEmpty()) {
            clearCoupon(cart);
            cart.setExpiresAt(null);
        } else {
            refreshExpiry(cart);
        }

        return toDto(cartRepository.save(cart));
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

        // ✅ FORCER LA SUPPRESSION DU COUPON
        clearCoupon(cart);

        cart.setExpiresAt(null);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
    }

    // ✅ METHODE VALIDATION COUPON (appelée par controlleur si besoin)
    public CouponValidateResponse validateCoupon(String code) {
        Optional<Coupon> opt = couponRepository.findByCodeIgnoreCase(code);
        if (opt.isPresent()) {
            Coupon c = opt.get();
            if(Boolean.TRUE.equals(c.getActive())) {
                return new CouponValidateResponse(c.getCode(), true, c.getPercent(), null);
            }
        }
        return new CouponValidateResponse(code, false, null, "Invalide");
    }

    // =========================
    // PRIVATE HELPERS
    // =========================

    private void clearCoupon(Cart cart) {
        cart.setCouponCode(null);
        cart.setCouponPercent(null);
    }

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
        clearCoupon(cart);
        cart.setExpiresAt(null);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
    }

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
                    // On envoie le prix effectif (promo incluse) pour affichage panier
                    .unitPrice(item.getUnitPrice() != null ? item.getUnitPrice() : effectivePrice(item.getProduct()))
                    .quantity(item.getQuantity())
                    .lineTotal(item.getLineTotal())
                    .imageUrl(imgUrl)
                    .stockQuantity(item.getProduct().getStockQuantity())
                    .build();
        }).collect(Collectors.toList());

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(itemsDto)
                .totalQuantity(totalQty)
                .totalAmount(BigDecimal.valueOf(totalAmt))
                .expiresAt(cart.getExpiresAt())
                // ✅ MAPPING COUPON
                .couponCode(cart.getCouponCode())
                .discountPercent(cart.getCouponPercent() != null ? cart.getCouponPercent() : 0)
                .build();
    }
}