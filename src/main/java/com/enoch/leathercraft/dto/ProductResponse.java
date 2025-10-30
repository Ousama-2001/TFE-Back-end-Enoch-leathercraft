// src/main/java/com/enoch/leathercraft/dto/ProductResponse.java
package com.enoch.leathercraft.dto;
import lombok.Builder; import lombok.Data;
import java.math.BigDecimal;
@Data @Builder
public class ProductResponse {
    private Long id; private String name; private String description;
    private BigDecimal price; private Integer stock;
}
