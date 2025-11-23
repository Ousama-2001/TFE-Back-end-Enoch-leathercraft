package com.enoch.leathercraft.controller;

import com.enoch.leathercraft.dto.ProductCreateRequest;
import com.enoch.leathercraft.dto.ProductResponse;
import com.enoch.leathercraft.services.ProductService;
import com.enoch.leathercraft.services.FileStorageService; // Import ajouté
import com.fasterxml.jackson.databind.ObjectMapper; // Import nécessaire pour désérialiser le JSON
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // Import nécessaire

import java.io.IOException;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService; // Renommé 'service' en 'productService' pour clarté
    private final FileStorageService fileStorageService; // NOUVEAUTÉ : Injection
    private final ObjectMapper objectMapper = new ObjectMapper(); // NOUVEAUTÉ : Pour la désérialisation du JSON

    // MODIFICATION DE LA METHODE CREATE POUR ACCEPTER LE FICHIER ET LE JSON
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ProductResponse> createProductWithImage(
            @RequestPart("product") String productJson, // Reçoit le DTO du produit en JSON stringifié
            @RequestPart("file") MultipartFile file) { // Reçoit le fichier image

        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build(); // Gérer si pas de fichier
        }

        try {
            // 1. Désérialisation du JSON en DTO
            ProductCreateRequest request = objectMapper.readValue(productJson, ProductCreateRequest.class);

            // 2. Stockage du fichier et récupération de son URL publique
            String imageUrl = fileStorageService.storeFile(file);

            // 3. Appel du service métier pour créer le produit et lier l'image
            ProductResponse response = productService.create(request, imageUrl);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IOException e) {
            // Erreur de désérialisation ou de lecture/écriture de fichier
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            // Autres erreurs (logique métier)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Ancienne méthode de création sans image (peut être supprimée ou conservée si utile)
    /*
    @PostMapping // L'ancienne méthode doit être supprimée ou avoir un chemin différent si vous utilisez la nouvelle
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductCreateRequest req) {
        return ResponseEntity.ok(service.create(req));
    }
    */

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductCreateRequest req
    ) {
        return ResponseEntity.ok(productService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}