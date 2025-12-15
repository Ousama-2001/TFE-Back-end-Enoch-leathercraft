// src/main/java/com/enoch/leathercraft/services/CatalogService.java
package com.enoch.leathercraft.services;

import com.enoch.leathercraft.dto.ProductResponse;
import com.enoch.leathercraft.entities.Product;
import com.enoch.leathercraft.entities.ProductImage;
import com.enoch.leathercraft.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final ProductRepository productRepository;

    /**
     * Retourne tous les produits actifs (non supprimés)
     * associés à la catégorie dont le slug = paramètre.
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategorySlug(String slug) {
        List<Product> products = productRepository.findActiveByCategorySlug(slug);
        return products.stream()
                .map(this::toDto)
                .toList();
    }

    // ====== Mapping Product -> ProductResponse ======
    private ProductResponse toDto(Product p) {

        // Images : primary d’abord, puis position
        List<String> imageUrls;

        if (p.getImages() != null && !p.getImages().isEmpty()) {
            imageUrls = p.getImages().stream()
                    .sorted((a, b) -> {
                        boolean ap = Boolean.TRUE.equals(a.getPrimary());
                        boolean bp = Boolean.TRUE.equals(b.getPrimary());

                        // primary d'abord
                        int cmpPrimary = Boolean.compare(bp, ap);
                        if (cmpPrimary != 0) return cmpPrimary;

                        // puis position
                        Integer pa = a.getPosition();
                        Integer pb = b.getPosition();

                        if (pa == null && pb == null) return 0;
                        if (pa == null) return 1;
                        if (pb == null) return -1;

                        return Integer.compare(pa, pb);
                    })
                    .map(ProductImage::getUrl)
                    .toList();
        } else {
            imageUrls = Collections.emptyList();
        }

        return ProductResponse.builder()
                .id(p.getId())
                .sku(p.getSku())
                .name(p.getName())
                .slug(p.getSlug())
                .description(p.getDescription())
                .price(p.getPrice())
                .currency(p.getCurrency())
                .weightGrams(p.getWeightGrams())
                .isActive(p.getIsActive())
                .stockQuantity(p.getStockQuantity())
                .imageUrls(imageUrls)
                .build();
    }
}
