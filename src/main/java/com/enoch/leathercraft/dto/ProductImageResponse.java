// src/main/java/com/enoch/leathercraft/dto/ProductImageResponse.java
package com.enoch.leathercraft.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductImageResponse {
    private Long id;
    private String url;
    private String altText;
    private Integer position;
    private Boolean isPrimary;
}
