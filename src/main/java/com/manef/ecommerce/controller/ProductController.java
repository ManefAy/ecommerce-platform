package com.manef.ecommerce.controller;

import com.manef.ecommerce.dto.request.ProductRequest;
import com.manef.ecommerce.dto.response.ProductResponse;
import com.manef.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for product endpoints.
 *
 * Public routes (no login required):
 * → GET /api/products                    → all active products
 * → GET /api/products/{id}               → single product by ID
 * → GET /api/products/slug/{slug}        → single product by slug
 * → GET /api/products/category/{id}      → products by category
 * → GET /api/products/search?keyword=x   → search products
 *
 * Admin only routes (ROLE_ADMIN required):
 * → GET    /api/products/admin/all       → all products including inactive
 * → POST   /api/products                 → create product
 * → PUT    /api/products/{id}            → update product
 * → DELETE /api/products/{id}            → delete product
 * → PATCH  /api/products/{id}/toggle     → toggle active status
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ─────────────────────────────────────────────────────
    // PUBLIC ENDPOINTS
    // ─────────────────────────────────────────────────────

    /**
     * Get all active products.
     * Used on storefront homepage and product listing.
     *
     * GET http://localhost:8080/api/products
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllActiveProducts() {
        return ResponseEntity.ok(productService.getAllActiveProducts());
    }

    /**
     * Get a single product by ID.
     *
     * GET http://localhost:8080/api/products/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    /**
     * Get a single product by slug.
     * Used on Next.js product detail page.
     * e.g. /products/nike-air-max-2024
     *
     * GET http://localhost:8080/api/products/slug/nike-air-max-2024
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ProductResponse> getProductBySlug(
            @PathVariable String slug) {
        return ResponseEntity.ok(productService.getProductBySlug(slug));
    }

    /**
     * Get all active products in a category.
     * Includes products in subcategories.
     *
     * GET http://localhost:8080/api/products/category/1
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(
            @PathVariable Long categoryId) {
        return ResponseEntity.ok(
                productService.getProductsByCategory(categoryId)
        );
    }

    /**
     * Search products by keyword.
     * Searches in title and description.
     *
     * GET http://localhost:8080/api/products/search?keyword=nike
     *
     * @RequestParam → reads the query parameter from the URL
     * e.g. ?keyword=nike extracts "nike"
     */
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(
            @RequestParam String keyword) {
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }

    // ─────────────────────────────────────────────────────
    // ADMIN ONLY ENDPOINTS
    // ─────────────────────────────────────────────────────

    /**
     * Get ALL products including inactive ones.
     * Used in admin dashboard product management.
     *
     * GET http://localhost:8080/api/products/admin/all
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    /**
     * Create a new product.
     * Requires ROLE_ADMIN.
     *
     * POST http://localhost:8080/api/products
     * Body: ProductRequest JSON
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(201)
                .body(productService.createProduct(request));
    }

    /**
     * Update an existing product.
     * Requires ROLE_ADMIN.
     *
     * PUT http://localhost:8080/api/products/1
     * Body: ProductRequest JSON
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    /**
     * Delete a product.
     * Requires ROLE_ADMIN.
     *
     * DELETE http://localhost:8080/api/products/1
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Toggle product active status.
     * Requires ROLE_ADMIN.
     *
     * PATCH http://localhost:8080/api/products/1/toggle
     *
     * @PatchMapping → used for PARTIAL updates
     * We're only changing one field (active)
     * not the entire product — so PATCH is more
     * semantically correct than PUT here
     */
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProductResponse> toggleProductStatus(
            @PathVariable Long id) {
        return ResponseEntity.ok(productService.toggleProductStatus(id));
    }
}