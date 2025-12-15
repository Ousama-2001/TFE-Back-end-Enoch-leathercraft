// src/main/java/com/enoch/leathercraft/services/ProductService.java
package com.enoch.leathercraft.services;

import com.enoch.leathercraft.dto.ProductCreateRequest;
import com.enoch.leathercraft.dto.ProductImageResponse;
import com.enoch.leathercraft.dto.ProductResponse;
import com.enoch.leathercraft.entities.Category;
import com.enoch.leathercraft.entities.Product;
import com.enoch.leathercraft.entities.ProductCategory;
import com.enoch.leathercraft.entities.ProductImage;
import com.enoch.leathercraft.repository.CategoryRepository;
import com.enoch.leathercraft.repository.ProductCategoryRepository;
import com.enoch.leathercraft.repository.ProductImageRepository;
import com.enoch.leathercraft.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repo;
    private final ProductImageRepository imageRepo;
    private final CategoryRepository categoryRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final FileStorageService fileStorageService;


    // =========================
    // ✅ PRODUITS ACTIFS
    // =========================
    public List<ProductResponse> getAll() {
        return repo.findByIsActiveTrueAndDeletedFalseOrderByNameAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public List<ProductResponse> getForCatalog(String segmentSlug, String categorySlug) {
        boolean hasSegment = segmentSlug != null && !segmentSlug.isBlank();
        boolean hasCategory = categorySlug != null && !categorySlug.isBlank();

        if (!hasSegment && !hasCategory) return getAll();

        List<Product> products;
        if (hasSegment && hasCategory) {
            products = repo.findActiveBySegmentAndCategory(segmentSlug, categorySlug);
        } else if (hasSegment) {
            products = repo.findActiveBySegmentSlug(segmentSlug);
        } else {
            products = repo.findActiveByCategorySlug(categorySlug);
        }

        return products.stream().map(this::toDto).toList();
    }

    public ProductResponse getById(Long id) {
        Product p = repo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));
        return toDto(p);
    }

    // =========================
    // ✅ CREATE (multi images)
    // =========================
    @Transactional
    public ProductResponse create(ProductCreateRequest req, List<String> imageUrls) {
        Product p = Product.builder()
                .sku(req.getSku())
                .name(req.getName())
                .slug(req.getSlug())
                .description(req.getDescription())
                .price(req.getPrice() != null ? req.getPrice().max(java.math.BigDecimal.ZERO) : java.math.BigDecimal.ZERO)
                .currency(req.getCurrency() != null ? req.getCurrency() : "EUR")
                .weightGrams(req.getWeightGrams())
                .isActive(req.getIsActive() != null ? req.getIsActive() : Boolean.TRUE)
                .stockQuantity(req.getStockQuantity() != null ? Math.max(req.getStockQuantity(), 0) : 0)
                .deleted(false)
                .build();

        if (imageUrls != null && !imageUrls.isEmpty()) {
            int pos = 0;
            for (String url : imageUrls) {
                ProductImage img = ProductImage.builder()
                        .url(url)
                        .altText(req.getName())
                        .position(pos)
                        .isPrimary(pos == 0)   // ✅ ICI (PAS .primary)
                        .build();
                p.addImage(img);
                pos++;
            }
        }

        p = repo.save(p);
        updateProductCategories(p.getId(), req.getSegmentCategoryId(), req.getTypeCategoryId());
        return toDto(p);
    }

    // =========================
    // ✅ UPDATE (ajoute images optionnelles)
    // =========================
    @Transactional
    public ProductResponse update(Long id, ProductCreateRequest req, List<String> newImageUrls) {
        Product existing = repo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

        existing.setSku(req.getSku());
        existing.setName(req.getName());
        existing.setSlug(req.getSlug());
        existing.setDescription(req.getDescription());
        existing.setPrice(req.getPrice() != null ? req.getPrice().max(java.math.BigDecimal.ZERO) : java.math.BigDecimal.ZERO);
        existing.setCurrency(req.getCurrency() != null ? req.getCurrency() : existing.getCurrency());
        existing.setWeightGrams(req.getWeightGrams());
        existing.setIsActive(req.getIsActive() != null ? req.getIsActive() : existing.getIsActive());
        existing.setUpdatedAt(Instant.now());

        if (req.getStockQuantity() != null) {
            existing.setStockQuantity(Math.max(req.getStockQuantity(), 0));
        }


        // ✅ ajoute des images (sans supprimer les anciennes)
        if (newImageUrls != null && !newImageUrls.isEmpty()) {
            int startPos = existing.getImages() != null ? existing.getImages().size() : 0;

            for (int i = 0; i < newImageUrls.size(); i++) {
                ProductImage img = ProductImage.builder()
                        .url(newImageUrls.get(i))
                        .altText(existing.getName())
                        .position(startPos + i)
                        .isPrimary(false)     // ✅ ICI (PAS .primary)
                        .build();
                existing.addImage(img);
            }

            // si aucune primaire -> mettre la 1ère en primaire
            boolean hasPrimary = existing.getImages().stream()
                    .anyMatch(im -> Boolean.TRUE.equals(im.getPrimary())); // compat ok
            if (!hasPrimary && !existing.getImages().isEmpty()) {
                existing.getImages().iterator().next().setPrimary(true);
            }
        }

        existing = repo.save(existing);
        updateProductCategories(existing.getId(), req.getSegmentCategoryId(), req.getTypeCategoryId());
        return toDto(existing);
    }

    // =========================
    // ✅ SOFT DELETE / ARCHIVE
    // =========================
    @Transactional
    public void delete(Long id) {
        Product p = repo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));
        p.setDeleted(true);
        p.setIsActive(false);
        p.setUpdatedAt(Instant.now());
        repo.save(p);
    }

    public List<ProductResponse> getArchived() {
        return repo.findByDeletedTrueOrderByUpdatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ProductResponse restore(Long id) {
        Product p = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));
        p.setDeleted(false);
        p.setIsActive(true);
        p.setUpdatedAt(Instant.now());
        repo.save(p);
        return toDto(p);
    }

    // =========================
    // ✅ STOCK
    // =========================
    @Transactional
    public ProductResponse updateStock(Long id, Integer newQty) {
        Product p = repo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));
        p.setStockQuantity(newQty != null ? newQty : 0);
        p.setUpdatedAt(Instant.now());
        repo.save(p);
        return toDto(p);
    }

    public List<ProductResponse> findLowStock(int threshold) {
        return repo.findByDeletedFalse().stream()
                .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() <= threshold)
                .map(this::toDto)
                .toList();
    }

    // ======================================================
    // ✅ CRUD IMAGES
    // ======================================================

    @Transactional
    public List<ProductImageResponse> addImages(Long productId, List<String> urls) {
        Product p = repo.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

        if (urls == null || urls.isEmpty()) return getImages(productId);

        int startPos = p.getImages() != null ? p.getImages().size() : 0;

        for (int i = 0; i < urls.size(); i++) {
            ProductImage img = ProductImage.builder()
                    .url(urls.get(i))
                    .altText(p.getName())
                    .position(startPos + i)
                    .isPrimary(false)     // ✅ ICI (PAS .primary)
                    .build();
            p.addImage(img);
        }

        boolean hasPrimary = p.getImages().stream().anyMatch(im -> Boolean.TRUE.equals(im.getPrimary()));
        if (!hasPrimary && !p.getImages().isEmpty()) {
            p.getImages().iterator().next().setPrimary(true);
        }

        repo.save(p);
        return getImages(productId);
    }

    @Transactional
    public void deleteImage(Long productId, Long imageId) {
        Product p = repo.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

        ProductImage img = imageRepo.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException("Image introuvable"));

        if (img.getProduct() == null || !Objects.equals(img.getProduct().getId(), p.getId())) {
            throw new EntityNotFoundException("Cette image n'appartient pas à ce produit");
        }

        boolean wasPrimary = Boolean.TRUE.equals(img.getPrimary());
        String urlToDelete = img.getUrl();

        // supprime DB
        p.getImages().remove(img);
        imageRepo.delete(img);
        repo.save(p);

        // ✅ réordonner positions
        List<ProductImage> remaining = imageRepo.findByProductIdOrderByPositionAsc(productId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setPosition(i);
        }

        // ✅ si primaire supprimée -> première devient primaire
        if (!remaining.isEmpty()) {
            boolean hasPrimary = remaining.stream().anyMatch(im -> Boolean.TRUE.equals(im.getPrimary()));
            if (!hasPrimary || wasPrimary) {
                remaining.forEach(im -> im.setPrimary(false));
                remaining.get(0).setPrimary(true);
            }
            imageRepo.saveAll(remaining);
        }
        // ✅ suppression physique du fichier

        fileStorageService.deleteByUrl(urlToDelete);
    }

    @Transactional
    public List<ProductImageResponse> setPrimaryImage(Long productId, Long imageId) {
        repo.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

        List<ProductImage> imgs = imageRepo.findByProductIdOrderByPositionAsc(productId);
        if (imgs.isEmpty()) return List.of();

        boolean found = false;
        for (ProductImage img : imgs) {
            if (Objects.equals(img.getId(), imageId)) {
                img.setPrimary(true);
                found = true;
            } else {
                img.setPrimary(false);
            }
        }

        if (!found) throw new EntityNotFoundException("Image introuvable pour ce produit");

        imageRepo.saveAll(imgs);
        return imgs.stream().map(this::toImageDto).toList();
    }

    @Transactional
    public List<ProductImageResponse> reorderImages(Long productId, List<Long> orderedImageIds) {
        repo.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

        List<ProductImage> imgs = imageRepo.findByProductIdOrderByPositionAsc(productId);
        if (imgs.isEmpty()) return List.of();

        Map<Long, ProductImage> map = imgs.stream()
                .collect(Collectors.toMap(ProductImage::getId, x -> x));

        int pos = 0;
        for (Long id : orderedImageIds) {
            ProductImage img = map.get(id);
            if (img != null) img.setPosition(pos++);
        }

        for (ProductImage img : imgs) {
            if (!orderedImageIds.contains(img.getId())) {
                img.setPosition(pos++);
            }
        }

        imageRepo.saveAll(imgs);

        List<ProductImage> sorted = imageRepo.findByProductIdOrderByPositionAsc(productId);
        return sorted.stream().map(this::toImageDto).toList();
    }

    public List<ProductImageResponse> getImages(Long productId) {
        return imageRepo.findByProductIdOrderByPositionAsc(productId)
                .stream()
                .map(this::toImageDto)
                .toList();
    }

    // =========================
    // ✅ CATEGORIES
    // =========================
    @Transactional
    protected void updateProductCategories(Long productId, Long segmentCategoryId, Long typeCategoryId) {
        productCategoryRepository.deleteByProductId(productId);

        if (segmentCategoryId != null) {
            checkCategoryExists(segmentCategoryId);
            productCategoryRepository.save(ProductCategory.builder()
                    .productId(productId)
                    .categoryId(segmentCategoryId)
                    .isPrimary(true)
                    .build());
        }

        if (typeCategoryId != null) {
            checkCategoryExists(typeCategoryId);
            productCategoryRepository.save(ProductCategory.builder()
                    .productId(productId)
                    .categoryId(typeCategoryId)
                    .isPrimary(false)
                    .build());
        }
    }

    private void checkCategoryExists(Long catId) {
        if (!categoryRepository.existsById(catId)) {
            throw new EntityNotFoundException("Catégorie introuvable (id=" + catId + ")");
        }
    }

    // =========================
    // ✅ DTO MAPPING
    // =========================
    private ProductResponse toDto(Product p) {
        List<ProductImage> imgs = imageRepo.findByProductIdOrderByPositionAsc(p.getId());
        List<String> urls = imgs.stream().map(ProductImage::getUrl).toList();

        List<ProductCategory> links = productCategoryRepository.findByProductId(p.getId());

        Long segmentCatId = null;
        Long typeCatId = null;

        for (ProductCategory link : links) {
            if (Boolean.TRUE.equals(link.getIsPrimary())) segmentCatId = link.getCategoryId();
            else typeCatId = link.getCategoryId();
        }

        String segmentSlug = null;
        String typeSlug = null;

        List<Long> catIds = new ArrayList<>();
        if (segmentCatId != null) catIds.add(segmentCatId);
        if (typeCatId != null) catIds.add(typeCatId);

        if (!catIds.isEmpty()) {
            List<Category> cats = categoryRepository.findAllById(catIds);
            for (Category c : cats) {
                if (segmentCatId != null && c.getId().equals(segmentCatId)) segmentSlug = c.getSlug();
                if (typeCatId != null && c.getId().equals(typeCatId)) typeSlug = c.getSlug();
            }
        }

        return ProductResponse.builder()
                .id(p.getId())
                .sku(p.getSku())
                .name(p.getName())
                .slug(p.getSlug())
                .description(p.getDescription())
                .price(p.getPrice())
                .currency(p.getCurrency())
                .weightGrams(p.getWeightGrams())
                .isActive(p.getIsActive())
                .imageUrls(urls)
                .images(imgs.stream().map(this::toImageDto).toList())
                .stockQuantity(p.getStockQuantity())
                .segmentCategoryId(segmentCatId)
                .typeCategoryId(typeCatId)
                .segmentSlug(segmentSlug)
                .typeSlug(typeSlug)
                .build();
    }

    private ProductImageResponse toImageDto(ProductImage img) {
        return ProductImageResponse.builder()
                .id(img.getId())
                .url(img.getUrl())
                .altText(img.getAltText())
                .position(img.getPosition())
                .isPrimary(img.getPrimary())
                .build();
    }
}
