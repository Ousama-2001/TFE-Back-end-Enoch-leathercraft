// src/main/java/com/enoch/leathercraft/entities/WishlistItem.java
package com.enoch.leathercraft.entities;

import com.enoch.leathercraft.auth.domain.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
// On ignore aussi les champs internes Hibernate au cas où
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ⬇️ on ne renvoie pas le user dans le JSON
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}
