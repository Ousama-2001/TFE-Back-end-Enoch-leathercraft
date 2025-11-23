package com.enoch.leathercraft.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer quantity;

    // CORRECTION 1 : Ajout du champ line_total (Total de la ligne)
    @Column(name = "line_total")
    private BigDecimal lineTotal;

    // CORRECTION 2 : Ajout du champ unit_price (Prix unitaire figé)
    @Column(name = "unit_price")
    private BigDecimal unitPrice;
}