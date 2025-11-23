package com.enoch.leathercraft.services;

import com.enoch.leathercraft.auth.domain.User;
import com.enoch.leathercraft.auth.repo.UserRepository;
import com.enoch.leathercraft.dto.CartAddRequest;
import com.enoch.leathercraft.dto.CartItemResponse;
import com.enoch.leathercraft.dto.CartResponse;
import com.enoch.leathercraft.dto.CartUpdateRequest;
import com.enoch.leathercraft.entities.*;
import com.enoch.leathercraft.repository.CartRepository;
import com.enoch.leathercraft.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public CartResponse getCurrentCart() {
        User user = getAuthenticatedUser();
        Cart cart = getOrCreateCart(user);
        return toDto(cart);
    }

    @Transactional
    public CartResponse addItem(CartAddRequest req) {
        User user = getAuthenticatedUser();
        Cart cart = getOrCreateCart(user);

        Product product = productRepository.findById(req.getProductId())
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            // Cas : Le produit est déjà là, on met à jour
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + req.getQuantity();
            item.setQuantity(newQuantity);

            // On met à jour le prix unitaire (au cas où il a changé en base)
            item.setUnitPrice(product.getPrice());

            // On recalcule le total ligne
            item.setLineTotal(product.getPrice().multiply(BigDecimal.valueOf(newQuantity)));

        } else {
            // Cas : Nouveau produit dans le panier
            BigDecimal total = product.getPrice().multiply(BigDecimal.valueOf(req.getQuantity()));

            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(req.getQuantity())
                    .unitPrice(product.getPrice()) // <--- CORRECTION CRUCIALE ICI
                    .lineTotal(total)
                    .build();
            cart.getItems().add(newItem);
        }

        cartRepository.save(cart);
        return toDto(cart);
    }

    @Transactional
    public CartResponse updateItem(Long productId, CartUpdateRequest req) {
        User user = getAuthenticatedUser();
        Cart cart = getOrCreateCart(user);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Article non trouvé"));

        // Mise à jour quantité
        item.setQuantity(req.getQuantity());

        // Mise à jour total ligne
        // Note: on utilise item.getUnitPrice() si on veut garder le prix figé,
        // ou item.getProduct().getPrice() si on veut le prix actuel. Ici je prends le prix actuel.
        BigDecimal newTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(req.getQuantity()));
        item.setLineTotal(newTotal);
        item.setUnitPrice(item.getProduct().getPrice()); // Mise à jour du prix unitaire aussi

        cartRepository.save(cart);
        return toDto(cart);
    }

    @Transactional
    public CartResponse removeItem(Long productId) {
        User user = getAuthenticatedUser();
        Cart cart = getOrCreateCart(user);

        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));

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
        cartRepository.save(cart);
    }

    // --- PRIVATE HELPERS ---

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
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse toDto(Cart cart) {
        // Calcul du total global
        double totalAmt = cart.getItems().stream()
                .mapToDouble(i -> {
                    // On privilégie le lineTotal stocké, sinon on le calcule
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
                    .build();
        }).collect(Collectors.toList());

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(itemsDto)
                .totalQuantity(totalQty)
                .totalAmount(BigDecimal.valueOf(totalAmt))
                .build();
    }
}