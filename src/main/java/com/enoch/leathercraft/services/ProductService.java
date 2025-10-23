package com.enoch.leathercraft.services;// ProductService.java

import com.enoch.leathercraft.dto.ProductCreateRequest;
import com.enoch.leathercraft.dto.ProductResponse;
import com.enoch.leathercraft.dto.ProductUpdateRequest;
import com.enoch.leathercraft.entities.Product;
import com.enoch.leathercraft.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    private final ProductRepository repo;

    private ProductResponse toResponse(Product p){
        return ProductResponse.builder()
                .id(p.getId()).name(p.getName()).description(p.getDescription())
                .price(p.getPrice()).stock(p.getStock()).build();
    }

    public ProductResponse create(ProductCreateRequest req){
        Product p = Product.builder()
                .name(req.getName()).description(req.getDescription())
                .price(req.getPrice()).stock(req.getStock()).build();
        return toResponse(repo.save(p));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String q, Pageable pageable){
        Page<Product> page = (q == null || q.isBlank())
                ? repo.findAll(pageable)
                : repo.findByNameContainingIgnoreCase(q, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id){
        Product p = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        return toResponse(p);
    }

    public ProductResponse update(Long id, ProductUpdateRequest req){
        Product p = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setPrice(req.getPrice());
        p.setStock(req.getStock());
        return toResponse(repo.save(p));
    }

    public void delete(Long id){
        if (!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        repo.deleteById(id);
    }
}
