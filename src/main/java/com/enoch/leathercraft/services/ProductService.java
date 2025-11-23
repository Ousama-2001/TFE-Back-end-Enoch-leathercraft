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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repo;
    private final ProductImageRepository imageRepo; // Injection de l'image repository

    // La méthode getAll fonctionne maintenant grâce à @EntityGraph dans le Repository
    public List<ProductResponse> getAll() {
        return repo.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public ProductResponse getById(Long id) {
        Product p = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));
        return toDto(p);
    }

    @Transactional
    public ProductResponse create(ProductCreateRequest req, String imageUrl) {
        // 1. Création et mapping des champs de base du Produit
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
                .build();

        // 2. Création de l'entité Image
        ProductImage image = ProductImage.builder()
                .url(imageUrl)
                .altText(req.getName() + " image")
                .position(0)
                .isPrimary(true)
                .build();

        // 3. Liaison de l'image au produit
        p.addImage(image);

        // 4. Sauvegarde (la cascade dans l'entité Product sauve aussi l'image)
        p = repo.save(p);

        return toDto(p);
    }

    @Transactional
    public ProductResponse update(Long id, ProductCreateRequest req) {
        // ... (Pas de changement) ...
        Product existing = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

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

        existing = repo.save(existing);
        return toDto(existing);
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new EntityNotFoundException("Produit introuvable");
        }
        repo.deleteById(id);
    }

    private ProductResponse toDto(Product p) {
        // MODIFICATION: Récupération de l'image pour le DTO
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
                .build();
    }
}