// src/main/java/com/enoch/leathercraft/entities/ProductCategoryId.java
package com.enoch.leathercraft.entities;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryId implements Serializable {
    private Long productId;
    private Long categoryId;
}
