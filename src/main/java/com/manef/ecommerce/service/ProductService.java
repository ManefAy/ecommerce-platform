package com.manef.ecommerce.service;

import com.manef.ecommerce.dto.request.ProductRequest;
import com.manef.ecommerce.dto.response.ProductResponse;
import com.manef.ecommerce.entity.Category;
import com.manef.ecommerce.entity.Product;
import com.manef.ecommerce.entity.StockMovement;
import com.manef.ecommerce.repository.CategoryRepository;
import com.manef.ecommerce.repository.ProductRepository;
import com.manef.ecommerce.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles all product business logic:
 * → Get all active products (storefront)
 * → Get products by category
 * → Search products by keyword
 * → Get single product by ID or slug
 * → Create product (admin)
 * → Update product (admin)
 * → Delete product (admin)
 * → Update stock (admin)
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockMovementRepository stockMovementRepository;

    // ─────────────────────────────────────────────────────
    // PUBLIC — STOREFRONT METHODS
    // ─────────────────────────────────────────────────────

    /**
     * Get all active products.
     * Used on the storefront homepage and product listing.
     */
    @Transactional
    public List<ProductResponse> getAllActiveProducts() {
        return productRepository.findByActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all active products in a category
     * including products in its subcategories.
     * Used on the category page in the storefront.
     */
    @Transactional
    public List<ProductResponse> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryOrSubCategory(categoryId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Search products by keyword in title or description.
     * Used in the storefront search bar.
     */
    @Transactional
    public List<ProductResponse> searchProducts(String keyword) {
        return productRepository.searchByKeyword(keyword)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a single product by its slug.
     * Used on the product detail page.
     * e.g. /products/nike-air-max-2024
     */
    @Transactional
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() ->
                        new RuntimeException("Product not found with slug: " + slug)
                );
        return mapToResponse(product);
    }

    /**
     * Get a single product by its ID.
     * Used in admin dashboard edit form.
     */
    @Transactional
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Product not found with id: " + id)
                );
        return mapToResponse(product);
    }

    // ─────────────────────────────────────────────────────
    // ADMIN ONLY METHODS
    // ─────────────────────────────────────────────────────

    /**
     * Get ALL products including inactive ones.
     * Used in admin dashboard product management table.
     */
    @Transactional
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create a new product.
     * Only accessible by ROLE_ADMIN.
     *
     * After creating the product we also create a
     * StockMovement record to track the initial stock.
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {

        // Check slug is not already taken
        if (productRepository.existsBySlug(request.getSlug())) {
            throw new RuntimeException(
                "Slug already exists: " + request.getSlug()
            );
        }

        // Load the category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() ->
                        new RuntimeException(
                            "Category not found with id: " + request.getCategoryId()
                        )
                );

        // Build and save the product
        Product product = Product.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .stockQuantity(request.getStockQuantity())
                .weight(request.getWeight())
                .slug(request.getSlug())
                .active(request.getActive())
                .category(category)
                .build();

        Product saved = productRepository.save(product);

        /**
         * Record the initial stock as a RESTOCK movement.
         * This gives us a complete stock history from day one.
         * quantityBefore = 0 (new product, no stock yet)
         * quantityAfter  = the initial stock quantity
         */
        StockMovement initialStock = StockMovement.builder()
                .product(saved)
                .quantityChanged(saved.getStockQuantity())
                .quantityBefore(0)
                .quantityAfter(saved.getStockQuantity())
                .reason(StockMovement.MovementReason.RESTOCK)
                .note("Initial stock when product was created")
                .build();

        stockMovementRepository.save(initialStock);

        return mapToResponse(saved);
    }

    /**
     * Update an existing product.
     * Only accessible by ROLE_ADMIN.
     */
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {

        // Find the existing product
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Product not found with id: " + id)
                );

        // Check slug uniqueness only if it changed
        if (!product.getSlug().equals(request.getSlug()) &&
             productRepository.existsBySlug(request.getSlug())) {
            throw new RuntimeException(
                "Slug already exists: " + request.getSlug()
            );
        }

        // Load the new category if it changed
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() ->
                        new RuntimeException(
                            "Category not found with id: " + request.getCategoryId()
                        )
                );

        /**
         * Check if stock quantity changed.
         * If it did → record a MANUAL_ADJUST stock movement.
         */
        int oldStock = product.getStockQuantity();
        int newStock = request.getStockQuantity();

        if (oldStock != newStock) {
            StockMovement adjustment = StockMovement.builder()
                    .product(product)
                    .quantityChanged(newStock - oldStock)
                    .quantityBefore(oldStock)
                    .quantityAfter(newStock)
                    .reason(StockMovement.MovementReason.MANUAL_ADJUST)
                    .note("Stock updated by admin during product edit")
                    .build();
            stockMovementRepository.save(adjustment);
        }

        // Update all product fields
        product.setTitle(request.getTitle());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setWeight(request.getWeight());
        product.setSlug(request.getSlug());
        product.setActive(request.getActive());
        product.setCategory(category);

        Product updated = productRepository.save(product);
        return mapToResponse(updated);
    }

    /**
     * Delete a product by ID.
     * Only accessible by ROLE_ADMIN.
     *
     * Note: we don't physically delete products often
     * in production — better to set active = false.
     * But this is here for admin flexibility.
     */
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Product not found with id: " + id)
                );
        productRepository.delete(product);
    }

    /**
     * Toggle product visibility.
     * Deactivate → hides from storefront without deleting.
     * Activate   → makes visible again.
     * Only accessible by ROLE_ADMIN.
     */
    @Transactional
    public ProductResponse toggleProductStatus(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Product not found with id: " + id)
                );

        // Flip the active status
        product.setActive(!product.getActive());
        Product updated = productRepository.save(product);
        return mapToResponse(updated);
    }

    // ─────────────────────────────────────────────────────
    // MAPPER — Entity to DTO
    // ─────────────────────────────────────────────────────

    /**
     * Converts a Product entity into a ProductResponse DTO.
     * Maps all fields including category info and images.
     */
    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .title(product.getTitle())
                .description(product.getDescription())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .stockQuantity(product.getStockQuantity())
                .weight(product.getWeight())
                .slug(product.getSlug())
                .active(product.getActive())
                // Category info
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                // Map images ordered by displayOrder
                .images(product.getImages() != null
                        ? product.getImages()
                                .stream()
                                .map(image -> ProductResponse.ImageResponse.builder()
                                        .id(image.getId())
                                        .imageUrl(image.getImageUrl())
                                        .displayOrder(image.getDisplayOrder())
                                        .build())
                                .collect(Collectors.toList())
                        : null)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}