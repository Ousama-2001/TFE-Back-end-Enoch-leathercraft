package com.enoch.leathercraft.services;

import com.enoch.leathercraft.dto.ProductCreateRequest;
import com.enoch.leathercraft.dto.ProductResponse;
import com.enoch.leathercraft.entities.Product;
import com.enoch.leathercraft.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository repo;

    public List<ProductResponse> getAll() {
        return repo.findAll().stream().map(this::toDto).toList();
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
        p.setCurrency(req.getCurrency());
        p.setWeightGrams(req.getWeightGrams());
        p.setIsActive(req.getIsActive() != null ? req.getIsActive() : Boolean.TRUE);

        p = repo.save(p);
        return toDto(p);
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
