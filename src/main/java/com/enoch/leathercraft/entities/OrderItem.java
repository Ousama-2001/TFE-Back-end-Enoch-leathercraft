package com.enoch.leathercraft.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    // On fige le nom et le prix au moment de l'achat
    // (Si le prix du produit change demain, l'historique de commande ne doit pas bouger)
    private String productName;
    private BigDecimal unitPrice;

    private Integer quantity;
}