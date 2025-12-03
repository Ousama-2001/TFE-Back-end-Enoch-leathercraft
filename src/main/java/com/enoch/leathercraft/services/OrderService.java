package com.enoch.leathercraft.services;

import com.enoch.leathercraft.dto.CheckoutRequest;
import com.enoch.leathercraft.dto.OrderItemResponse;
import com.enoch.leathercraft.dto.OrderResponse;
import com.enoch.leathercraft.entities.*;
import com.enoch.leathercraft.repository.CartRepository;
import com.enoch.leathercraft.repository.OrderRepository;
import com.enoch.leathercraft.repository.ProductRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
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
    private final ProductRepository productRepository;
    private final MailService mailService;

    // ------------------------------------------------------
    // CREATE ORDER (SANS ENVOYER L'EMAIL ICI)
    // ------------------------------------------------------
    @Transactional
    public OrderResponse createOrderFromCart(String userEmail, CheckoutRequest checkoutRequest) {
        Cart cart = cartRepository.findByUser_EmailAndStatus(userEmail, CartStatus.OPEN)
                .orElseThrow(() -> new EntityNotFoundException("Aucun panier ouvert trouvé pour cet utilisateur"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Impossible de commander un panier vide");
        }

        Order order = new Order();
        order.setReference("CMD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCustomerEmail(userEmail); // email du compte

        // 🔹 Infos de checkout (si tu as ajouté ces champs dans l'entité Order)
        if (checkoutRequest != null) {
            order.setFirstName(checkoutRequest.firstName());
            order.setLastName(checkoutRequest.lastName());
            order.setPhone(checkoutRequest.phone());
            order.setStreet(checkoutRequest.street());
            order.setPostalCode(checkoutRequest.postalCode());
            order.setCity(checkoutRequest.city());
            order.setCountry(checkoutRequest.country());
            order.setNotes(checkoutRequest.notes());
        }

        order.setStatus(OrderStatus.PENDING);

        BigDecimal totalAmount = BigDecimal.ZERO;
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

        // ❌ On NE PASSE PLUS l'email ici
        // mailService.sendOrderConfirmation(savedOrder);

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

    @Transactional(readOnly = true)
    public OrderResponse getUserOrderById(Long orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Commande introuvable"));

        if (!order.getCustomerEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("Cette commande n'appartient pas à cet utilisateur");
        }

        return toDto(order);
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

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Commande introuvable"));
        return toDto(order);
    }

    // ------------------------------------------------------
    // ADMIN : UPDATE STATUS
    // ------------------------------------------------------
    @Transactional
    public OrderResponse updateStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Commande introuvable"));

        final OrderStatus statusEnum;
        try {
            statusEnum = OrderStatus.valueOf(newStatus);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Statut invalide : " + newStatus);
        }

        order.setStatus(statusEnum);
        Order saved = orderRepository.save(order);

        if (statusEnum == OrderStatus.SHIPPED || statusEnum == OrderStatus.DELIVERED) {
            mailService.sendOrderStatusUpdated(saved);
        }

        return toDto(saved);
    }

    // ------------------------------------------------------
    // STRIPE : CONFIRMER LE PAIEMENT + ENVOYER EMAIL
    // ------------------------------------------------------
    @Transactional
    public OrderResponse markOrderAsPaidFromStripeSession(String sessionId) throws StripeException {
        // 1) Récupérer la session Stripe
        Session session = Session.retrieve(sessionId);

        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            throw new IllegalStateException("Paiement non confirmé par Stripe.");
        }

        // 2) Récupérer la référence de commande dans les métadonnées
        String orderReference = session.getMetadata().get("orderReference");
        if (orderReference == null) {
            throw new IllegalStateException("Référence de commande manquante dans la session Stripe.");
        }

        // 3) Retrouver la commande
        Order order = orderRepository.findByReference(orderReference)
                .orElseThrow(() -> new EntityNotFoundException("Commande introuvable pour la référence : " + orderReference));

        // 4) Si pas encore PAID, on met à jour + on envoie l'email
        if (order.getStatus() != OrderStatus.PAID) {
            order.setStatus(OrderStatus.PAID);
            Order saved = orderRepository.save(order);

            // ✅ Email de confirmation après paiement
            mailService.sendOrderConfirmation(saved);

            return toDto(saved);
        }

        return toDto(order);
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
