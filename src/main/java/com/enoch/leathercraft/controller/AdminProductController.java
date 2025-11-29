package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.dto.ProductCreateRequest;
import com.enoch.leathercraft.dto.ProductResponse;
import com.enoch.leathercraft.dto.StockUpdateRequest;
import com.enoch.leathercraft.services.FileStorageService;
import com.enoch.leathercraft.services.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // CREATE (Déjà fonctionnel)
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ProductResponse> createProductWithImage(
            @RequestPart("product") String productJson,
            @RequestPart("file") MultipartFile file) {
        try {
            ProductCreateRequest request = objectMapper.readValue(productJson, ProductCreateRequest.class);
            String imageUrl = fileStorageService.storeFile(file);
            ProductResponse response = productService.create(request, imageUrl);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // UPDATE (MODIFIÉ pour accepter une image optionnelle)
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ProductResponse> updateProductWithImage(
            @PathVariable Long id,
            @RequestPart("product") String productJson,
            @RequestPart(value = "file", required = false) MultipartFile file) { // Fichier optionnel

        try {
            // 1. Désérialiser le JSON
            ProductCreateRequest request = objectMapper.readValue(productJson, ProductCreateRequest.class);

            // 2. Traiter l'image SI elle est présente
            String imageUrl = null;
            if (file != null && !file.isEmpty()) {
                imageUrl = fileStorageService.storeFile(file);
            }

            // 3. Appeler le service update (qui gérera l'image si imageUrl n'est pas null)
            ProductResponse response = productService.update(id, request, imageUrl);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
    // --- Mise à jour du stock (gestion stock admin)
    @PutMapping("/{id}/stock")
    public ProductResponse updateStock(
            @PathVariable Long id,
            @RequestParam("quantity") Integer quantity
    ) {
        return productService.updateStock(id, quantity);
    }

    @GetMapping("/low-stock")
    public List<ProductResponse> lowStock(
            @RequestParam(defaultValue = "5") int threshold
    ) {
        return productService.findLowStock(threshold);
    }
}