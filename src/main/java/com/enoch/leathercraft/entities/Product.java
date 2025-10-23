// Product.java
package com.enoch.leathercraft.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    private String description;
    @Column(nullable = false) private Double price;
    @Column(nullable = false) private Integer stock;
}
