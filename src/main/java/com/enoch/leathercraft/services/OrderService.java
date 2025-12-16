package com.enoch.leathercraft.services;

import com.enoch.leathercraft.dto.*;
import com.enoch.leathercraft.entities.*;
import com.enoch.leathercraft.repository.CartRepository;
import com.enoch.leathercraft.repository.CouponRepository;
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
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final MailService mailService;
    private final StripeService stripeService;

    private final InvoicePdfService invoicePdfService;
    private final ReturnLabelPdfService returnLabelPdfService;

    // ------------------------------------------------------
    // CREATE ORDER FROM CART
    // ------------------------------------------------------
    @Transactional
    public OrderResponse createOrderFromCart(String userEmail, CheckoutRequest checkoutRequest) {
        // 1. Récupération du panier
        Cart cart = cartRepository.findByUser_EmailAndStatus(userEmail, CartStatus.OPEN)
                .orElseThrow(() -> new EntityNotFoundException("Aucun panier ouvert trouvé"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Impossible de commander un panier vide");
        }

        Order order = new Order();
        order.setReference("CMD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCustomerEmail(userEmail);
        order.setStatus(OrderStatus.PENDING);

        // Mapping adresses
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

        // 2. Calcul du SOUS-TOTAL (Prix des articles sans réduction coupon)
        BigDecimal subTotalAmount = BigDecimal.ZERO;
        Set<Product> updatedProducts = new HashSet<>();

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            int quantity = cartItem.getQuantity();

            int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            if (stock < quantity) throw new IllegalStateException("Stock insuffisant pour " + product.getName());

            product.setStockQuantity(stock - quantity);
            updatedProducts.add(product);

            // ✅ CORRECTION ICI : On récupère le PRIX EFFECTIF (Promo ou Base)
            BigDecimal effectiveUnitPrice = product.getEffectivePrice();

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .unitPrice(effectiveUnitPrice) // On stocke le prix promo si applicable
                    .quantity(quantity)
                    .build();

            order.addItem(orderItem);

            // On ajoute au total : Prix Effectif * Quantité
            subTotalAmount = subTotalAmount.add(effectiveUnitPrice.multiply(BigDecimal.valueOf(quantity)));
        }

        // 3. APPLICATION DU COUPON (Si code présent)
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = subTotalAmount;

        if (checkoutRequest != null && checkoutRequest.promoCode() != null && !checkoutRequest.promoCode().isBlank()) {
            String code = checkoutRequest.promoCode().trim();
            Optional<Coupon> couponOpt = couponRepository.findByCodeIgnoreCase(code);

            if (couponOpt.isPresent()) {
                Coupon coupon = couponOpt.get();

                // Vérification validité coupon
                Instant now = Instant.now();
                boolean isActive = Boolean.TRUE.equals(coupon.getActive());
                boolean started = coupon.getStartsAt() == null || !now.isBefore(coupon.getStartsAt());
                boolean notExpired = coupon.getEndsAt() == null || !now.isAfter(coupon.getEndsAt());

                if (isActive && started && notExpired) {
                    Integer percent = coupon.getPercent();

                    if (percent != null && percent > 0) {
                        BigDecimal pct = BigDecimal.valueOf(percent);
                        // discount = subtotal * (percent / 100)
                        discountAmount = subTotalAmount.multiply(pct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                        order.setCouponCode(coupon.getCode());
                        order.setCouponPercent(percent);

                        // Incrémenter le compteur d'utilisation
                        if(coupon.getUsedCount() == null) coupon.setUsedCount(0);
                        coupon.setUsedCount(coupon.getUsedCount() + 1);
                        couponRepository.save(coupon);
                    }
                }
            }
        }

        // Total final
        totalAmount = subTotalAmount.subtract(discountAmount).max(BigDecimal.ZERO);

        // 4. Sauvegarde
        order.setSubtotalAmount(subTotalAmount);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);
        productRepository.saveAll(updatedProducts);

        // Vider le panier
        cartService.clearCart(userEmail);

        return toDto(savedOrder);
    }

    // ------------------------------------------------------
    // CLIENT : MES COMMANDES
    // ------------------------------------------------------
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(String userEmail) {
        return orderRepository.findByCustomerEmailOrderByCreatedAtDesc(userEmail)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getUserOrderById(Long orderId, String userEmail) {
        return toDto(getOrderForUserOrThrow(orderId, userEmail));
    }

    // ------------------------------------------------------
    // ADMIN
    // ------------------------------------------------------
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Commande introuvable"));
        return toDto(order);
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Commande introuvable"));

        OrderStatus status;
        try {
            status = OrderStatus.valueOf(newStatus);
        } catch (Exception e) {
            throw new IllegalArgumentException("Statut invalide : " + newStatus);
        }

        order.setStatus(status);
        Order saved = orderRepository.save(order);

        if (status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED) {
            mailService.sendOrderStatusUpdated(saved);
        }

        return toDto(saved);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, String userEmail) {
        Order order = getOrderForUserOrThrow(orderId, userEmail);

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Commande non annulable");
        }

        OrderStatus previous = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);

        if (previous == OrderStatus.PAID) {
            mailService.sendPaidOrderCancelledToCustomer(saved);
            mailService.sendPaidOrderCancelledToAdmins(saved);
        }

        return toDto(saved);
    }

    // ------------------------------------------------------
    // STRIPE
    // ------------------------------------------------------
    @Transactional
    public StripeCheckoutResponse createStripeCheckoutForOrder(Long orderId, String userEmail)
            throws StripeException {
        Order order = getOrderForUserOrThrow(orderId, userEmail);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Commande non payable");
        }
        String checkoutUrl = stripeService.createCheckoutSession(toDto(order));
        return new StripeCheckoutResponse(checkoutUrl, order.getReference());
    }

    @Transactional
    public OrderResponse markOrderAsPaidFromStripeSession(String sessionId) throws StripeException {
        Session session = Session.retrieve(sessionId);

        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            throw new IllegalStateException("Paiement non confirmé");
        }

        String ref = session.getMetadata().get("orderReference");
        Order order = orderRepository.findByReference(ref)
                .orElseThrow(() -> new EntityNotFoundException("Commande introuvable"));

        if (order.getStatus() != OrderStatus.PAID) {
            order.setStatus(OrderStatus.PAID);
            Order saved = orderRepository.save(order);
            mailService.sendOrderConfirmation(saved);
            return toDto(saved);
        }
        return toDto(order);
    }

    // ------------------------------------------------------
    // RETOURS
    // ------------------------------------------------------
    @Transactional
    public OrderResponse requestReturn(Long orderId, String userEmail, ReturnRequest request) {
        Order order = getOrderForUserOrThrow(orderId, userEmail);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Retour non autorisé");
        }

        StringBuilder notes = new StringBuilder(Optional.ofNullable(order.getNotes()).orElse(""));
        notes.append("\n\n=== DEMANDE DE RETOUR ===\n");
        notes.append("Motif : ").append(request.reason()).append("\n");
        if (request.comment() != null && !request.comment().isBlank()) {
            notes.append("Commentaire : ").append(request.comment()).append("\n");
        }

        order.setNotes(notes.toString());
        order.setStatus(OrderStatus.RETURN_REQUESTED);

        Order saved = orderRepository.save(order);
        mailService.sendReturnRequested(saved);
        return toDto(saved);
    }

    @Transactional
    public OrderResponse approveReturnFromAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Commande introuvable"));
        if (order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new IllegalStateException("Retour non acceptable");
        }
        order.setStatus(OrderStatus.RETURN_APPROVED);
        Order saved = orderRepository.save(order);
        mailService.sendReturnApprovedToCustomer(saved);
        return toDto(saved);
    }

    @Transactional
    public OrderResponse rejectReturnFromAdmin(Long orderId, String adminReason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Commande introuvable"));
        if (order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new IllegalStateException("Retour non refusables");
        }
        if (adminReason == null || adminReason.isBlank()) {
            throw new IllegalArgumentException("Une raison de refus est obligatoire.");
        }

        StringBuilder notes = new StringBuilder(Optional.ofNullable(order.getNotes()).orElse(""));
        notes.append("\n\n=== RETOUR REFUSÉ PAR L'ADMIN ===\n");
        notes.append("Raison : ").append(adminReason).append("\n");
        order.setNotes(notes.toString());

        order.setStatus(OrderStatus.RETURN_REJECTED);
        Order saved = orderRepository.save(order);
        mailService.sendReturnRejectedToCustomer(saved, adminReason);
        return toDto(saved);
    }

    // ------------------------------------------------------
    // PDFS
    // ------------------------------------------------------
    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(Long orderId, String userEmail) {
        Order order = getOrderForUserOrThrow(orderId, userEmail);
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Facture indisponible");
        }
        return invoicePdfService.generate(order);
    }

    @Transactional(readOnly = true)
    public byte[] generateReturnLabelPdf(Long orderId, String userEmail) {
        Order order = getOrderForUserOrThrow(orderId, userEmail);
        if (order.getStatus() != OrderStatus.RETURN_APPROVED) {
            throw new IllegalStateException("Le bon de retour est disponible uniquement si le retour est approuvé.");
        }
        return returnLabelPdfService.generate(order);
    }

    // ------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------
    private Order getOrderForUserOrThrow(Long orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Commande introuvable"));
        if (!order.getCustomerEmail().equalsIgnoreCase(userEmail)) {
            throw new AccessDeniedException("Accès refusé");
        }
        return order;
    }

    private OrderResponse toDto(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> OrderItemResponse.builder()
                        .productName(i.getProductName())
                        .unitPrice(i.getUnitPrice())
                        .quantity(i.getQuantity())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .reference(order.getReference())
                .totalAmount(order.getTotalAmount())
                .subtotalAmount(order.getSubtotalAmount())
                .discountAmount(order.getDiscountAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .notes(order.getNotes())
                .couponCode(order.getCouponCode())
                .couponPercent(order.getCouponPercent())
                .items(items)
                .customerEmail(order.getCustomerEmail())
                .userId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .build();
    }
}