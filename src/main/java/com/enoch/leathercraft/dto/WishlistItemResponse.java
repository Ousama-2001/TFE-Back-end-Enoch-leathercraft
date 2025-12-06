package com.enoch.leathercraft.dto;

import com.enoch.leathercraft.entities.Product;
import com.enoch.leathercraft.entities.ProductImage;
import com.enoch.leathercraft.entities.WishlistItem;

import java.time.Instant;
import java.util.List;

public record WishlistItemResponse(
        Long id,
        ProductResponse product,
        Instant createdAt
) {

    public static WishlistItemResponse fromEntity(WishlistItem entity) {

        Product p = entity.getProduct();

        // 🔥 On transforme Set<ProductImage> -> List<String> (urls)
        List<String> imageUrls = (p.getImages() == null)
                ? List.of()
                : p.getImages().stream()
                .map(ProductImage::getUrl)
                .toList();

        ProductResponse productResponse = ProductResponse.builder()
                .id(p.getId())
                .sku(p.getSku())
                .name(p.getName())
                .slug(p.getSlug())
                .description(p.getDescription())
                .material(p.getMaterial())
                .price(p.getPrice())
                .currency(p.getCurrency())
                .weightGrams(p.getWeightGrams())
                .isActive(p.getIsActive())
                .stockQuantity(p.getStockQuantity())
                .imageUrls(imageUrls)   // ✅ ICI : plus de getImageUrls()
                .build();

        return new WishlistItemResponse(
                entity.getId(),
                productResponse,
                entity.getCreatedAt()
        );
    }
}
