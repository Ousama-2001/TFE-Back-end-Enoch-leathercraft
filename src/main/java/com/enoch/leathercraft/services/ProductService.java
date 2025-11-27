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

    public List<ProductResponse> getAll() {
        return repo.findAll().stream().map(this::toDto).toList();
    }

    public ProductResponse getById(Long id) {
        Product p = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));
        return toDto(p);
    }

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
                .build();

        ProductImage image = ProductImage.builder()
                .url(imageUrl)
                .altText(req.getName())
                .position(0)
                .isPrimary(true)
                .build();

        p.addImage(image);
        p = repo.save(p);
        return toDto(p);
    }

    @Transactional
    public ProductResponse update(Long id, ProductCreateRequest req, String newImageUrl) {
        Product existing = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

        // Mise à jour des champs texte
        existing.setSku(req.getSku());
        existing.setName(req.getName());
        existing.setSlug(req.getSlug());
        existing.setDescription(req.getDescription());
        existing.setMaterial(req.getMaterial());
        existing.setPrice(req.getPrice());
        existing.setCurrency(req.getCurrency() != null ? req.getCurrency() : existing.getCurrency());
        existing.setWeightGrams(req.getWeightGrams());
        existing.setIsActive(req.getIsActive() != null ? req.getIsActive() : existing.getIsActive());
        existing.setUpdatedAt(Instant.now());

        // Stock : si fourni, on remplace, sinon on garde l'ancien
        if (req.getStockQuantity() != null) {
            existing.setStockQuantity(req.getStockQuantity());
        }

        // LOGIQUE MISE À JOUR IMAGE
        if (newImageUrl != null) {
            Set<ProductImage> images = existing.getImages();

            if (images != null && !images.isEmpty()) {
                ProductImage img = images.iterator().next();
                img.setUrl(newImageUrl);
            } else {
                ProductImage newImg = ProductImage.builder()
                        .url(newImageUrl)
                        .altText(req.getName())
                        .position(0)
                        .isPrimary(true)
                        .build();
                existing.addImage(newImg);
            }
        }

        existing = repo.save(existing);
        return toDto(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new EntityNotFoundException("Produit introuvable");
        repo.deleteById(id);
    }

    // ------- STOCK ADMIN (pour modifier uniquement la quantité) -------
    @Transactional
    public ProductResponse updateStock(Long productId, Integer newQuantity) {
        Product p = repo.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));
        p.setStockQuantity(newQuantity != null ? newQuantity : 0);
        p.setUpdatedAt(Instant.now());
        p = repo.save(p);
        return toDto(p);
    }

    // ------- Produits en stock bas -------
    public List<ProductResponse> findLowStock(int threshold) {
        return repo.findAll().stream()
                .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() <= threshold)
                .map(this::toDto)
                .toList();
    }

    private ProductResponse toDto(Product p) {
        List<String> imageUrls = p.getImages().stream()
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
                .imageUrls(imageUrls)
                .stockQuantity(p.getStockQuantity())
                .build();
    }
}
