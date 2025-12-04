package com.enoch.leathercraft.entities;

import com.enoch.leathercraft.entities.Product;
import com.enoch.leathercraft.auth.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wishlist_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "product_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    @ManyToOne(optional = false)
    private Product product;
}
