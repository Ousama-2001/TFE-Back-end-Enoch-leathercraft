package com.enoch.leathercraft.services;

import com.enoch.leathercraft.dto.ProductCreateRequest;
import com.enoch.leathercraft.dto.ProductUpdateRequest;
import com.enoch.leathercraft.dto.ProductResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    public List<ProductResponse> getAll() { /* ... */ return List.of(); }
    public ProductResponse getById(Long id) { /* ... */ return null; }
    public ProductResponse create(ProductCreateRequest req) { /* ... */ return null; }
    public ProductResponse update(Long id, ProductUpdateRequest req) { /* ... */ return null; }
    public void delete(Long id) { /* ... */ }
}
