// src/main/java/com/enoch/leathercraft/controller/ProductController.java
package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.dto.ProductResponse;
import com.enoch.leathercraft.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Liste publique des produits
     * Possibilité de filtrer par segment + catégorie :
     *   /api/products?segment=homme
     *   /api/products?segment=homme&category=sacs-sacoches
     *   /api/products?category=portefeuilles
     */
    @GetMapping
    public List<ProductResponse> list(
            @RequestParam(name = "segment", required = false) String segment,
            @RequestParam(name = "category", required = false) String category
    ) {
        return productService.getForCatalog(segment, category);
    }

    @GetMapping("/{id}")
    public ProductResponse getOne(@PathVariable Long id) {
        return productService.getById(id);
    }
}
