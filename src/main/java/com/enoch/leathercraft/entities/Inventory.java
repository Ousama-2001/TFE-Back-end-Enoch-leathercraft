// com/enoch/leathercraft/catalog/domain/Inventory.java
package com.enoch.leathercraft.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="inventory")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Inventory {
    @Id @Column(name="product_id") private Long productId;

    private Integer quantity;
    private Integer lowStockThreshold;
}
