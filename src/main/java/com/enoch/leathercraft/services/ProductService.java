package com.enoch.leathercraft.services;

import com.enoch.leathercraft.dto.ProductCreateRequest;
import com.enoch.leathercraft.dto.ProductResponse;
import com.enoch.leathercraft.entities.Product;
import com.enoch.leathercraft.entities.ProductImage;
import com.enoch.leathercraft.repository.ProductImageRepository;
import com.enoch.leathercraft.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repo;
    private final ProductImageRepository imageRepo;

    // === Tous les produits actifs ===
    public List<ProductResponse> getAll() {
        return repo.findByIsActiveTrueAndDeletedFalseOrderByNameAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    // === Produits par catégorie (slug) ===
    public List<ProductResponse> getByCategorySlug(String slug) {
        return repo.findActiveByCategorySlug(slug)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public ProductResponse getById(Long id) {
        Product p = repo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));
        return toDto(p);
    }

    // === CREATE ===
    @Transactional
    public ProductResponse create(ProductCreateRequest req, String imageUrl) {
        Product p = Product.builder()
                .sku(req.getSku())
                .name(req.getName())
                .slug(req.getSlug())
                .description(req.getDescription())
                .material(req.getMaterial())
                .price(req.getPrice())
                .currency(req.getCurrency() != null ? req.getCurrency() : "EUR")
                .weightGrams(req.getWeightGrams())
                .isActive(req.getIsActive() != null ? req.getIsActive() : Boolean.TRUE)
                .stockQuantity(req.getStockQuantity() != null ? req.getStockQuantity() : 0)
                .deleted(false)
                .build();

        if (imageUrl != null) {
            ProductImage img = ProductImage.builder()
                    .url(imageUrl)
                    .altText(req.getName())
                    .position(0)
                    .isPrimary(true)
                    .build();
            p.addImage(img);
        }

        p = repo.save(p);
        return toDto(p);
    }

    // === UPDATE ===
    @Transactional
    public ProductResponse update(Long id, ProductCreateRequest req, String newImageUrl) {
        Product existing = repo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

        existing.setSku(req.getSku());
        existing.setName(req.getName());
        existing.setSlug(req.getSlug());
        existing.setDescription(req.getDescription());
        existing.setMaterial(req.getMaterial());
        existing.setPrice(req.getPrice());
        existing.setCurrency(
                req.getCurrency() != null ? req.getCurrency() : existing.getCurrency()
        );
        existing.setWeightGrams(req.getWeightGrams());
        existing.setIsActive(
                req.getIsActive() != null ? req.getIsActive() : existing.getIsActive()
        );
        existing.setUpdatedAt(Instant.now());

        if (req.getStockQuantity() != null) {
            existing.setStockQuantity(req.getStockQuantity());
        }

        if (newImageUrl != null) {
            Set<ProductImage> images = existing.getImages();
            if (!images.isEmpty()) {
                ProductImage img = images.iterator().next();
                img.setUrl(newImageUrl);
                img.setAltText(existing.getName());
            } else {
                ProductImage img = ProductImage.builder()
                        .url(newImageUrl)
                        .altText(existing.getName())
                        .position(0)
                        .isPrimary(true)
                        .build();
                existing.addImage(img);
            }
        }

        existing = repo.save(existing);
        return toDto(existing);
    }

    // === SOFT DELETE ===
    @Transactional
    public void delete(Long id) {
        Product p = repo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

        p.setDeleted(true);
        p.setIsActive(false);
        p.setUpdatedAt(Instant.now());
        repo.save(p);
    }

    // === STOCK ===
    @Transactional
    public ProductResponse updateStock(Long id, Integer newQty) {
        Product p = repo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

        p.setStockQuantity(newQty != null ? newQty : 0);
        p.setUpdatedAt(Instant.now());
        p = repo.save(p);

        return toDto(p);
    }

    // === Produits avec stock faible (pour l'admin) ===
    public List<ProductResponse> findLowStock(int threshold) {
        return repo.findByDeletedFalse().stream()
                .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() <= threshold)
                .map(this::toDto)
                .toList();
    }

    // === Produits archivés ===
    public List<ProductResponse> getArchived() {
        return repo.findByDeletedTrueOrderByUpdatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    // === Restaurer ===
    @Transactional
    public ProductResponse restore(Long id) {
        Product p = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

        p.setDeleted(false);
        p.setIsActive(true);
        p.setUpdatedAt(Instant.now());
        p = repo.save(p);

        return toDto(p);
    }

    // === DTO ===
    private ProductResponse toDto(Product p) {
        List<String> urls = p.getImages().stream()
                .map(ProductImage::getUrl)
                .collect(Collectors.toList());

        return ProductResponse.builder()
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
                .imageUrls(urls)
                .stockQuantity(p.getStockQuantity())
                .build();
    }
}
