// src/main/java/com/enoch/leathercraft/entities/WishlistItem.java
package com.enoch.leathercraft.entities;

import com.enoch.leathercraft.auth.domain.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wishlist_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ lien vers USER
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({
            "passwordHash",
            "createdAt",
            "updatedAt",
            "wishlistItems"
    })
    private User user;

    // ✅ lien vers PRODUCT
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({
            "images",
            "wishlistItems"
    })
    private Product product;
}
