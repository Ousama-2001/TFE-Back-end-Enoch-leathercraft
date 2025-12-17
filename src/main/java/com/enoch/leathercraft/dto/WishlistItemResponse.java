// src/main/java/com/enoch/leathercraft/dto/WishlistItemResponse.java
package com.enoch.leathercraft.dto;

import com.enoch.leathercraft.entities.Product;
import com.enoch.leathercraft.entities.ProductImage;
import com.enoch.leathercraft.entities.WishlistItem;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public record WishlistItemResponse(
        Long id,
        ProductResponse product,
        Instant createdAt
) {
    public static WishlistItemResponse fromEntity(WishlistItem entity) {
        Product p = entity.getProduct();

        // On transforme les ProductImage -> List<String> d’URL
        List<String> imageUrls = (p.getImages() == null)
                ? Collections.emptyList()
                : p.getImages()
                .stream()
                .map(ProductImage::getUrl)   // "/uploads/products/xxx.jpg"
                .collect(Collectors.toList());

        ProductResponse productDto = ProductResponse.builder()
                .id(p.getId())
                .sku(p.getSku())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .currency(p.getCurrency())
                .weightGrams(p.getWeightGrams())
                .isActive(p.getIsActive())
                .imageUrls(imageUrls)
                .stockQuantity(p.getStockQuantity())
                .build();

        return new WishlistItemResponse(
                entity.getId(),
                productDto,
                entity.getCreatedAt()
        );
    }
}
