// src/main/java/com/enoch/leathercraft/controller/CatalogController.java
package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.dto.ProductResponse;
import com.enoch.leathercraft.services.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200") // ajuste si besoin
public class CatalogController {

    private final CatalogService catalogService;

    /**
     * Produits actifs par catégorie (slug).
     * Exemple : GET /api/catalog/category/homme
     */
    @GetMapping("/category/{slug}")
    public ResponseEntity<List<ProductResponse>> getByCategory(
            @PathVariable String slug
    ) {
        List<ProductResponse> products = catalogService.getProductsByCategorySlug(slug);
        return ResponseEntity.ok(products);
    }
}
