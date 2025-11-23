package com.enoch.leathercraft.services;

import com.enoch.leathercraft.dto.OrderItemResponse;
import com.enoch.leathercraft.dto.OrderResponse;
import com.enoch.leathercraft.entities.*;
import com.enoch.leathercraft.repository.CartRepository;
import com.enoch.leathercraft.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;

    @Transactional
    public OrderResponse createOrderFromCart(String userEmail) {
        // 1. Récupérer le panier "OPEN" de l'utilisateur via son email
        Cart cart = cartRepository.findByUser_EmailAndStatus(userEmail, CartStatus.OPEN)
                .orElseThrow(() -> new EntityNotFoundException("Aucun panier ouvert trouvé pour cet utilisateur"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Impossible de commander un panier vide");
        }

        // 2. Créer la commande
        Order order = new Order();
        order.setReference("CMD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCustomerEmail(userEmail);
        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 3. Transférer les items du Panier vers la Commande
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .unitPrice(product.getPrice()) // On fige le prix au moment de l'achat
                    .quantity(cartItem.getQuantity())
                    .build();

            // Calcul du sous-total de la ligne
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);

            // Lier l'item à la commande
            order.addItem(orderItem);
        }

        order.setTotalAmount(totalAmount);

        // 4. Sauvegarder la commande en base
        Order savedOrder = orderRepository.save(order);

        // 5. Vider le panier
        cartService.clearCart(userEmail);

        // 6. Retourner le DTO
        return toDto(savedOrder);
    }

    // Nouvelle méthode pour récupérer l'historique
    public List<OrderResponse> getUserOrders(String userEmail) {
        return orderRepository.findByCustomerEmailOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Mapping Entité -> DTO
    private OrderResponse toDto(Order order) {
        // Conversion des items de commande
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
                .items(itemsDto) // Ajout des items dans la réponse
                .build();
    }
}