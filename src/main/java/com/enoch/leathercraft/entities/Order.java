package com.enoch.leathercraft.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Référence unique (ex: CMD-123456)
    @Column(unique = true, nullable = false)
    private String reference;

    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    // L'email du client qui a passé la commande (pour l'historique)
    private String customerEmail;

    // La liste des produits achetés
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        if (this.status == null) this.status = OrderStatus.PENDING;
    }

    // Méthode utilitaire pour lier les items
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}