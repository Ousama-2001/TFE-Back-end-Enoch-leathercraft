package com.enoch.leathercraft.services;

import com.enoch.leathercraft.dto.ProductCreateRequest;
import com.enoch.leathercraft.dto.ProductResponse;
import com.enoch.leathercraft.entities.Product;
import com.enoch.leathercraft.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repo;

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
    public ProductResponse create(ProductCreateRequest req) {
        Product p = new Product();
        p.setSku(req.getSku());
        p.setName(req.getName());
        p.setSlug(req.getSlug());
        p.setDescription(req.getDescription());
        p.setMaterial(req.getMaterial());
        p.setPrice(req.getPrice());
        p.setCurrency(req.getCurrency() != null ? req.getCurrency() : "EUR");
        p.setWeightGrams(req.getWeightGrams());
        p.setIsActive(req.getIsActive() != null ? req.getIsActive() : Boolean.TRUE);

        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());

        p = repo.save(p);
        return toDto(p);
    }

    @Transactional
    public ProductResponse update(Long id, ProductCreateRequest req) {
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
                .build();
    }
}
