package com.enoch.leathercraft.services;

import com.enoch.leathercraft.dto.OrderItemResponse;
import com.enoch.leathercraft.dto.OrderResponse;
import com.enoch.leathercraft.entities.*;
import com.enoch.leathercraft.repository.CartRepository;
import com.enoch.leathercraft.repository.OrderRepository;
import com.enoch.leathercraft.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;

    // 👉 pour mettre à jour le stock
    private final ProductRepository productRepository;

    // ------------------------------------------------------
    // CREATE ORDER
    // ------------------------------------------------------
    @Transactional
    public OrderResponse createOrderFromCart(String userEmail) {
        Cart cart = cartRepository.findByUser_EmailAndStatus(userEmail, CartStatus.OPEN)
                .orElseThrow(() -> new EntityNotFoundException("Aucun panier ouvert trouvé pour cet utilisateur"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Impossible de commander un panier vide");
        }

        Order order = new Order();
        order.setReference("CMD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCustomerEmail(userEmail);
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;

        // Pour stocker les produits dont on met à jour le stock
        Set<Product> updatedProducts = new HashSet<>();

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            int quantity = cartItem.getQuantity();

            // --- Vérifier le stock ---
            int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;

            if (currentStock < quantity) {
                throw new IllegalStateException(
                        "Stock insuffisant pour le produit : " + product.getName()
                );
            }

            // Décrémenter le stock
            product.setStockQuantity(currentStock - quantity);
            updatedProducts.add(product);

            // Créer la ligne de commande
            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .unitPrice(product.getPrice())
                    .quantity(quantity)
                    .build();

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            totalAmount = totalAmount.add(lineTotal);
            order.addItem(orderItem);
        }

        order.setTotalAmount(totalAmount);

        // Sauvegarde commande
        Order savedOrder = orderRepository.save(order);

        // Sauvegarde des produits avec stock mis à jour
        productRepository.saveAll(updatedProducts);

        // Vider le panier
        cartService.clearCart(userEmail);

        return toDto(savedOrder);
    }

    // ------------------------------------------------------
    // CLIENT : GET USER ORDERS
    // ------------------------------------------------------
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(String userEmail) {
        return orderRepository
                .findByCustomerEmailOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------
    // ADMIN : GET ALL ORDERS
    // ------------------------------------------------------
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------
    // ADMIN : UPDATE STATUS
    // ------------------------------------------------------
    @Transactional
    public OrderResponse updateStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Commande introuvable"));

        try {
            order.setStatus(OrderStatus.valueOf(newStatus));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Statut invalide : " + newStatus);
        }

        return toDto(orderRepository.save(order));
    }

    // ------------------------------------------------------
    // DTO MAPPER
    // ------------------------------------------------------
    private OrderResponse toDto(Order order) {
        List<OrderItemResponse> itemsDto = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .productName(item.getProductName())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .reference(order.getReference())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .items(itemsDto)
                .build();
    }
}
