// src/main/java/com/enoch/leathercraft/services/ProductService.java
package com.enoch.leathercraft.services;

import com.enoch.leathercraft.dto.ProductCreateRequest;
import com.enoch.leathercraft.dto.ProductResponse;
import com.enoch.leathercraft.entities.Product;
import com.enoch.leathercraft.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service @RequiredArgsConstructor
public class ProductService {
    private final ProductRepository repo;

    public List<ProductResponse> getAll() {
        return repo.findAll().stream().map(this::toDto).toList();
    }
    public ProductResponse create(ProductCreateRequest req) {
        var p = Product.builder()
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .stock(req.getStock())
                .build();
        return toDto(repo.save(p));
    }
    private ProductResponse toDto(Product p) {
        return ProductResponse.builder()
                .id(p.getId()).name(p.getName()).description(p.getDescription())
                .price(p.getPrice()).stock(p.getStock()).build();
    }
}
