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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
    // ✅ CREATE
    // =========================
    @Transactional
    public ProductResponse create(ProductCreateRequest req, List<String> imageUrls) {

        normalizePromo(req);

        Instant promoStart = startOfDayUtc(req.getPromoStartAt());
        Instant promoEnd = endOfDayUtc(req.getPromoEndAt());

        Product p = Product.builder()
                .sku(req.getSku())
                .name(req.getName())
                .description(req.getDescription())
                .material(req.getMaterial())

                .price(safeMoney(req.getPrice()))
                .currency((req.getCurrency() == null || req.getCurrency().isBlank()) ? "EUR" : req.getCurrency())
                .weightGrams(req.getWeightGrams())

                .isActive(req.getIsActive() != null ? req.getIsActive() : Boolean.TRUE)
                .stockQuantity(req.getStockQuantity() != null ? Math.max(req.getStockQuantity(), 0) : 0)

                // ✅ promo
                .promoPrice(req.getPromoPrice())
                .promoStartAt(promoStart)
                .promoEndAt(promoEnd)

                .deleted(false)
                .build();

        // ✅ images
        if (imageUrls != null && !imageUrls.isEmpty()) {
            int pos = 0;
            for (String url : imageUrls) {
                if (url == null || url.isBlank()) continue;

                ProductImage img = ProductImage.builder()
                        .url(url)
                        .altText(req.getName())
                        .position(pos)
                        .isPrimary(pos == 0)
                        .build();
                p.addImage(img);
                pos++;
            }
        }

        ensurePrimaryImage(p);

        p = repo.save(p);
        updateProductCategories(p.getId(), req.getSegmentCategoryId(), req.getTypeCategoryId());
        return toDto(p);
    }

    // =========================
    // ✅ UPDATE
    // =========================
    @Transactional
    public ProductResponse update(Long id, ProductCreateRequest req, List<String> newImageUrls) {
        Product existing = repo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

        normalizePromo(req);

        existing.setSku(req.getSku());
        existing.setName(req.getName());
        existing.setDescription(req.getDescription());
        existing.setMaterial(req.getMaterial());

        existing.setPrice(safeMoney(req.getPrice()));

        if (req.getCurrency() != null && !req.getCurrency().isBlank()) {
            existing.setCurrency(req.getCurrency());
        } else if (existing.getCurrency() == null || existing.getCurrency().isBlank()) {
            existing.setCurrency("EUR");
        }

        existing.setWeightGrams(req.getWeightGrams());
        if (req.getIsActive() != null) existing.setIsActive(req.getIsActive());

        if (req.getStockQuantity() != null) {
            existing.setStockQuantity(Math.max(req.getStockQuantity(), 0));
        }

        // ✅ promo
        existing.setPromoPrice(req.getPromoPrice());
        existing.setPromoStartAt(startOfDayUtc(req.getPromoStartAt()));
        existing.setPromoEndAt(endOfDayUtc(req.getPromoEndAt()));

        existing.setUpdatedAt(Instant.now());

        // ✅ ajoute des images (sans supprimer les anciennes)
        if (newImageUrls != null && !newImageUrls.isEmpty()) {
            int startPos = existing.getImages() != null ? existing.getImages().size() : 0;

            int added = 0;
            for (String url : newImageUrls) {
                if (url == null || url.isBlank()) continue;

                ProductImage img = ProductImage.builder()
                        .url(url)
                        .altText(existing.getName())
                        .position(startPos + added)
                        .isPrimary(false)
                        .build();
                existing.addImage(img);
                added++;
            }
        }

        ensurePrimaryImage(existing);

        existing = repo.save(existing);
        updateProductCategories(existing.getId(), req.getSegmentCategoryId(), req.getTypeCategoryId());
        return toDto(existing);
    }

    // =========================
    // ✅ ARCHIVE / RESTORE
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

        int qty = (newQty == null) ? 0 : Math.max(newQty, 0);
        p.setStockQuantity(qty);

        p.setUpdatedAt(Instant.now());
        repo.save(p);
        return toDto(p);
    }

    public List<ProductResponse> findLowStock(int threshold) {
        int th = Math.max(threshold, 0);

        return repo.findByDeletedFalse().stream()
                .filter(p -> p.getStockQuantity() != null && p.getStockQuantity() <= th)
                .map(this::toDto)
                .toList();
    }

    // =========================
    // ✅ IMAGES CRUD
    // =========================
    @Transactional
    public List<ProductImageResponse> addImages(Long productId, List<String> urls) {
        Product p = repo.findByIdAndDeletedFalse(productId)
                .orElseThrow(() -> new EntityNotFoundException("Produit introuvable"));

        if (urls == null || urls.isEmpty()) return getImages(productId);

        int startPos = p.getImages() != null ? p.getImages().size() : 0;
        int added = 0;

        for (String url : urls) {
            if (url == null || url.isBlank()) continue;

            ProductImage img = ProductImage.builder()
                    .url(url)
                    .altText(p.getName())
                    .position(startPos + added)
                    .isPrimary(false)
                    .build();
            p.addImage(img);
            added++;
        }

        ensurePrimaryImage(p);

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

        p.getImages().remove(img);
        imageRepo.delete(img);
        repo.save(p);

        List<ProductImage> remaining = imageRepo.findByProductIdOrderByPositionAsc(productId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setPosition(i);
        }

        if (!remaining.isEmpty()) {
            boolean hasPrimary = remaining.stream().anyMatch(im -> Boolean.TRUE.equals(im.getPrimary()));
            if (!hasPrimary || wasPrimary) {
                remaining.forEach(im -> im.setPrimary(false));
                remaining.get(0).setPrimary(true);
            }
            imageRepo.saveAll(remaining);
        }

        fileStorageService.deleteByUrl(urlToDelete);
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

        // ceux non listés => à la fin
        for (ProductImage img : imgs) {
            if (!orderedImageIds.contains(img.getId())) {
                img.setPosition(pos++);
            }
        }

        imageRepo.saveAll(imgs);

        // on renvoie trié
        List<ProductImage> sorted = imageRepo.findByProductIdOrderByPositionAsc(productId);
        return sorted.stream().map(this::toImageDto).toList();
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
                .description(p.getDescription())
                .price(p.getPrice())

                .promoPrice(p.getPromoPrice())
                .promoStartAt(toLocalDateUtc(p.getPromoStartAt()))
                .promoEndAt(toLocalDateUtc(p.getPromoEndAt()))

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

    // =========================
    // ✅ HELPERS
    // =========================
    private BigDecimal safeMoney(BigDecimal v) {
        if (v == null) return BigDecimal.ZERO;
        return v.max(BigDecimal.ZERO);
    }

    private void ensurePrimaryImage(Product p) {
        if (p.getImages() == null || p.getImages().isEmpty()) return;

        boolean hasPrimary = p.getImages().stream().anyMatch(im -> Boolean.TRUE.equals(im.getPrimary()));
        if (!hasPrimary) {
            ProductImage first = p.getImages().stream()
                    .min(Comparator.comparingInt(im -> im.getPosition() == null ? 999999 : im.getPosition()))
                    .orElse(null);
            if (first != null) first.setPrimary(true);
        }
    }

    /**
     * Nettoie/corrige la promo :
     * - promoPrice null/0 => on supprime start/end
     * - start/end inversées => swap
     */
    private void normalizePromo(ProductCreateRequest req) {
        if (req == null) return;

        BigDecimal promo = req.getPromoPrice();
        if (promo == null || promo.compareTo(BigDecimal.ZERO) <= 0) {
            req.setPromoPrice(null);
            req.setPromoStartAt(null);
            req.setPromoEndAt(null);
            return;
        }

        req.setPromoPrice(promo.max(BigDecimal.ZERO));

        LocalDate start = req.getPromoStartAt();
        LocalDate end = req.getPromoEndAt();

        if (start != null && end != null && end.isBefore(start)) {
            req.setPromoStartAt(end);
            req.setPromoEndAt(start);
        }
    }

    // =========================
    // ✅ DATE CONVERSIONS (Option B)
    // =========================
    private Instant startOfDayUtc(LocalDate d) {
        return d == null ? null : d.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Instant endOfDayUtc(LocalDate d) {
        return d == null ? null : d.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusMillis(1);
    }

    private LocalDate toLocalDateUtc(Instant i) {
        return i == null ? null : i.atZone(ZoneOffset.UTC).toLocalDate();
    }
}
