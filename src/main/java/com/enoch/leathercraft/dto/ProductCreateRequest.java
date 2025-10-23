// ProductCreateRequest.java
package com.enoch.leathercraft.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductCreateRequest {
    @NotBlank private String name;
    private String description;
    @NotNull @PositiveOrZero private Double price;
    @NotNull @PositiveOrZero private Integer stock;
}
