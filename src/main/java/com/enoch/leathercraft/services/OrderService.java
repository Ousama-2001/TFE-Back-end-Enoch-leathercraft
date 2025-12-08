// src/main/java/com/enoch/leathercraft/services/OrderService.java
package com.enoch.leathercraft.services;

import com.enoch.leathercraft.dto.CheckoutRequest;
import com.enoch.leathercraft.dto.OrderItemResponse;
import com.enoch.leathercraft.dto.OrderResponse;
import com.enoch.leathercraft.dto.StripeCheckoutResponse;
import com.enoch.leathercraft.dto.ReturnRequest;
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
    private final StripeService stripeService;

    // ------------------------------------------------------
    // CREATE ORDER DEPUIS PANIER
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
        order.setCustomerEmail(userEmail);

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

            int currentStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;

            if (currentStock < quantity) {
                throw new IllegalStateException("Stock insuffisant pour le produit : " + product.getName());
            }

            product.setStockQuantity(currentStock - quantity);
            updatedProducts.add(product);

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

        Order savedOrder = orderRepository.save(order);
        productRepository.saveAll(updatedProducts);

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
    // ADMIN : LISTE + DETAIL
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
    // STRIPE : CONFIRMER PAIEMENT (callback)
    // ------------------------------------------------------
    @Transactional
    public OrderResponse markOrderAsPaidFromStripeSession(String sessionId) throws StripeException {
        Session session = Session.retrieve(sessionId);

        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            throw new IllegalStateException("Paiement non confirmé par Stripe.");
        }

        String orderReference = session.getMetadata().get("orderReference");
        if (orderReference == null) {
            throw new IllegalStateException("Référence de commande manquante dans la session Stripe.");
        }

        Order order = orderRepository.findByReference(orderReference)
                .orElseThrow(() -> new EntityNotFoundException("Commande introuvable pour la référence : " + orderReference));

        if (order.getStatus() != OrderStatus.PAID) {
            order.setStatus(OrderStatus.PAID);
            Order saved = orderRepository.save(order);
            mailService.sendOrderConfirmation(saved);
            return toDto(saved);
        }

        return toDto(order);
    }

    // ------------------------------------------------------
    // HELPER : récupérer commande + contrôle email
    // ------------------------------------------------------
    private Order getOrderForUserOrThrow(Long orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Commande introuvable"));

        if (!order.getCustomerEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("Vous n'avez pas accès à cette commande.");
        }

        return order;
    }

    // ------------------------------------------------------
    // ANNULER UNE COMMANDE (PENDING ou PAID) + mails si remboursée
    // ------------------------------------------------------
    @Transactional
    public OrderResponse cancelOrder(Long orderId, String userEmail) {
        Order order = getOrderForUserOrThrow(orderId, userEmail);

        OrderStatus previousStatus = order.getStatus();

        if (previousStatus != OrderStatus.PENDING
                && previousStatus != OrderStatus.PAID) {
            throw new IllegalStateException("Cette commande ne peut plus être annulée.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        // 💸 Si la commande était payée, on considère que c’est une annulation / remboursement
        if (previousStatus == OrderStatus.PAID) {
            mailService.sendPaidOrderCancelledToCustomer(saved);
            mailService.sendPaidOrderCancelledToAdmins(saved);
        }

        return toDto(saved);
    }

    // ------------------------------------------------------
    // 🔥 DEMANDER UN RETOUR (commande livrée)
    // ------------------------------------------------------
    @Transactional
    public OrderResponse requestReturn(Long orderId, String userEmail, ReturnRequest request) {
        Order order = getOrderForUserOrThrow(orderId, userEmail);

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Seules les commandes livrées peuvent faire l'objet d'un retour.");
        }

        StringBuilder notes = new StringBuilder(
                order.getNotes() != null ? order.getNotes() + "\n\n" : ""
        );
        notes.append("=== DEMANDE DE RETOUR ===\n");
        notes.append("Motif : ").append(request.reason() != null ? request.reason() : "Non précisé").append("\n");
        if (request.comment() != null && !request.comment().isBlank()) {
            notes.append("Commentaire : ").append(request.comment()).append("\n");
        }
        order.setNotes(notes.toString());

        order.setStatus(OrderStatus.RETURN_REQUESTED);
        Order saved = orderRepository.save(order);

        mailService.sendReturnRequested(saved);

        return toDto(saved);
    }

    // ------------------------------------------------------
    // ADMIN : ACCEPTER UN RETOUR
    // ------------------------------------------------------
    @Transactional
    public OrderResponse approveReturnFromAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Commande introuvable"));

        if (order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new IllegalStateException("Seules les commandes avec un retour demandé peuvent être acceptées.");
        }

        StringBuilder notes = new StringBuilder(
                order.getNotes() != null ? order.getNotes() + "\n\n" : ""
        );
        notes.append("=== RETOUR ACCEPTÉ PAR L'ADMIN ===\n");
        notes.append("Adresse de retour communiquée au client.\n");
        order.setNotes(notes.toString());

        order.setStatus(OrderStatus.RETURN_APPROVED);
        Order saved = orderRepository.save(order);

        mailService.sendReturnApprovedToCustomer(saved);

        return toDto(saved);
    }

    // ------------------------------------------------------
// ADMIN : REFUSER UN RETOUR
// ------------------------------------------------------
    @Transactional
    public OrderResponse rejectReturnFromAdmin(Long orderId, String adminReason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Commande introuvable"));

        if (order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new IllegalStateException("Seules les commandes avec un retour demandé peuvent être refusées.");
        }

        if (adminReason == null || adminReason.isBlank()) {
            throw new IllegalArgumentException("Une raison de refus est obligatoire.");
        }

        StringBuilder notes = new StringBuilder(
                order.getNotes() != null ? order.getNotes() + "\n\n" : ""
        );
        notes.append("=== RETOUR REFUSÉ PAR L'ADMIN ===\n");
        notes.append("Raison : ").append(adminReason).append("\n"); // ✅ ici sans ()
        order.setNotes(notes.toString());

        order.setStatus(OrderStatus.RETURN_REJECTED);
        Order saved = orderRepository.save(order);

        mailService.sendReturnRejectedToCustomer(saved, adminReason);

        return toDto(saved);
    }


    // ------------------------------------------------------
    // 🔥 CRÉER SESSION STRIPE POUR COMMANDE EXISTANTE
    // ------------------------------------------------------
    @Transactional
    public StripeCheckoutResponse createStripeCheckoutForOrder(Long orderId, String userEmail)
            throws StripeException {

        Order order = getOrderForUserOrThrow(orderId, userEmail);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Seules les commandes en attente peuvent être payées.");
        }

        OrderResponse dto = toDto(order);
        String checkoutUrl = stripeService.createCheckoutSession(dto);

        return new StripeCheckoutResponse(checkoutUrl, order.getReference());
    }

    // ------------------------------------------------------
    // FACTURE TXT
    // ------------------------------------------------------
    public String generateInvoiceContent(Long orderId, String userEmail) {
        Order order = getOrderForUserOrThrow(orderId, userEmail);

        if (order.getStatus() != OrderStatus.PAID
                && order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("La facture n'est disponible que pour les commandes payées.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("FACTURE - Enoch Leathercraft Shop\n");
        sb.append("=================================\n\n");
        sb.append("Référence commande : ").append(order.getReference()).append("\n");
        sb.append("Client : ").append(order.getFirstName())
                .append(" ").append(order.getLastName()).append("\n");
        sb.append("Email : ").append(order.getCustomerEmail()).append("\n");
        sb.append("Date : ").append(order.getCreatedAt()).append("\n\n");

        sb.append("Adresse de livraison :\n");
        sb.append(order.getStreet()).append("\n");
        sb.append(order.getPostalCode()).append(" ").append(order.getCity()).append("\n");
        sb.append(order.getCountry()).append("\n\n");

        sb.append("Détail :\n");
        for (OrderItem item : order.getItems()) {
            sb.append(" - ")
                    .append(item.getProductName())
                    .append(" x")
                    .append(item.getQuantity())
                    .append(" @ ")
                    .append(item.getUnitPrice())
                    .append(" EUR\n");
        }

        sb.append("\nMontant total : ")
                .append(order.getTotalAmount())
                .append(" EUR\n");

        sb.append("Statut : ").append(order.getStatus()).append("\n\n");
        sb.append("Merci pour votre achat !\n");

        return sb.toString();
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
                .notes(order.getNotes())
                .items(itemsDto)
                .build();
    }
}
