// src/main/java/com/enoch/leathercraft/controller/AdminProductController.java
package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.dto.ProductCreateRequest;
import com.enoch.leathercraft.dto.ProductImageResponse;
import com.enoch.leathercraft.dto.ProductResponse;
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

    // ✅ IMPORTANT : ObjectMapper injecté (config Spring => LocalDate OK)
    private final ObjectMapper objectMapper;

    // =========================
    // ✅ CREATE : multi images
    // FRONT: FormData -> product (JSON) + files (many)
    // =========================
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ProductResponse> createProduct(
            @RequestPart("product") String productJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestPart(value = "file", required = false) MultipartFile legacyFile
    ) {
        try {
            ProductCreateRequest req = objectMapper.readValue(productJson, ProductCreateRequest.class);

            // compat ancien front: "file"
            if ((files == null || files.isEmpty()) && legacyFile != null && !legacyFile.isEmpty()) {
                files = List.of(legacyFile);
            }

            List<String> urls = (files != null && !files.isEmpty())
                    ? fileStorageService.storeFiles(files)
                    : List.of();

            ProductResponse response = productService.create(req, urls);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =========================
    // ✅ UPDATE : ajoute des images (optionnel)
    // FRONT: product (JSON) + files (optionnel)
    // =========================
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestPart("product") String productJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestPart(value = "file", required = false) MultipartFile legacyFile
    ) {
        try {
            ProductCreateRequest req = objectMapper.readValue(productJson, ProductCreateRequest.class);

            if ((files == null || files.isEmpty()) && legacyFile != null && !legacyFile.isEmpty()) {
                files = List.of(legacyFile);
            }

            List<String> urls = (files != null && !files.isEmpty())
                    ? fileStorageService.storeFiles(files)
                    : List.of();

            ProductResponse response = productService.update(id, req, urls);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ✅ SOFT DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ Produits archivés
    @GetMapping("/archived")
    public List<ProductResponse> getArchived() {
        return productService.getArchived();
    }

    // ✅ Restaurer
    @PatchMapping("/{id}/restore")
    public ProductResponse restore(@PathVariable Long id) {
        return productService.restore(id);
    }

    // ✅ Stock
    @PutMapping("/{id}/stock")
    public ProductResponse updateStock(
            @PathVariable Long id,
            @RequestParam("quantity") Integer quantity
    ) {
        return productService.updateStock(id, quantity);
    }

    @GetMapping("/low-stock")
    public List<ProductResponse> lowStock(@RequestParam(defaultValue = "5") int threshold) {
        return productService.findLowStock(threshold);
    }

    // ======================================================
    // 🔥 ENDPOINTS CRUD IMAGES
    // ======================================================

    // ✅ ajouter des images à un produit existant
    @PostMapping(value = "/{id}/images", consumes = {"multipart/form-data"})
    public ResponseEntity<List<ProductImageResponse>> addImages(
            @PathVariable Long id,
            @RequestPart("files") List<MultipartFile> files
    ) {
        try {
            List<String> urls = fileStorageService.storeFiles(files);
            return ResponseEntity.ok(productService.addImages(id, urls));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ✅ supprimer une image
    @DeleteMapping("/{productId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long productId,
            @PathVariable Long imageId
    ) {
        productService.deleteImage(productId, imageId);
        return ResponseEntity.noContent().build();
    }

    // ✅ définir l'image principale
    @PutMapping("/{productId}/images/{imageId}/primary")
    public ResponseEntity<List<ProductImageResponse>> setPrimary(
            @PathVariable Long productId,
            @PathVariable Long imageId
    ) {
        return ResponseEntity.ok(productService.setPrimaryImage(productId, imageId));
    }

    // ✅ réordonner les images
    @PutMapping("/{productId}/images/reorder")
    public ResponseEntity<List<ProductImageResponse>> reorder(
            @PathVariable Long productId,
            @RequestBody List<Long> orderedImageIds
    ) {
        return ResponseEntity.ok(productService.reorderImages(productId, orderedImageIds));
    }

    // ✅ récupérer les images d’un produit
    @GetMapping("/{id}/images")
    public List<ProductImageResponse> getImages(@PathVariable Long id) {
        return productService.getImages(id);
    }
}
