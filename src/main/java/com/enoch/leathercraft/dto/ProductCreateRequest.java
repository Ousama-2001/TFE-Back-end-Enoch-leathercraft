// src/main/java/com/enoch/leathercraft/dto/ProductCreateRequest.java
package com.enoch.leathercraft.dto;
import jakarta.validation.constraints.*; import lombok.Data;
import java.math.BigDecimal;
@Data
public class ProductCreateRequest {
    @NotBlank private String name;
    @NotNull private BigDecimal price;
    @NotNull private Integer stock;
    private String description;
}
