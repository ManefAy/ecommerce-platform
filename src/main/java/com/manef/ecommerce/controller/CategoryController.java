package com.manef.ecommerce.controller;

import com.manef.ecommerce.dto.request.CategoryRequest;
import com.manef.ecommerce.dto.response.CategoryResponse;
import com.manef.ecommerce.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for category endpoints.
 *
 * Public routes (no login required):
 * → GET /api/categories
 * → GET /api/categories/{id}
 * → GET /api/categories/slug/{slug}
 *
 * Admin only routes (ROLE_ADMIN required):
 * → POST   /api/categories
 * → PUT    /api/categories/{id}
 * → DELETE /api/categories/{id}
 *
 * @PreAuthorize("hasAuthority('ROLE_ADMIN')") →
 * checks the JWT token role before allowing access.
 * If ROLE_USER tries to access → 403 Forbidden
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // ─────────────────────────────────────────────────────
    // PUBLIC ENDPOINTS
    // ─────────────────────────────────────────────────────

    /**
     * Get all top-level categories with subcategories.
     * Used in storefront navigation menu.
     *
     * GET http://localhost:8080/api/categories
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    /**
     * Get a single category by ID.
     *
     * GET http://localhost:8080/api/categories/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(
            @PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    /**
     * Get a single category by slug.
     * Used in Next.js: /category/electronics
     *
     * GET http://localhost:8080/api/categories/slug/electronics
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<CategoryResponse> getCategoryBySlug(
            @PathVariable String slug) {
        return ResponseEntity.ok(categoryService.getCategoryBySlug(slug));
    }

    // ─────────────────────────────────────────────────────
    // ADMIN ONLY ENDPOINTS
    // ─────────────────────────────────────────────────────

    /**
     * Create a new category.
     * Requires ROLE_ADMIN.
     *
     * POST http://localhost:8080/api/categories
     * Body: CategoryRequest JSON
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(201)
                .body(categoryService.createCategory(request));
    }

    /**
     * Update an existing category.
     * Requires ROLE_ADMIN.
     *
     * PUT http://localhost:8080/api/categories/1
     * Body: CategoryRequest JSON
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    /**
     * Delete a category.
     * Requires ROLE_ADMIN.
     *
     * DELETE http://localhost:8080/api/categories/1
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        /**
         * ResponseEntity.noContent() → HTTP 204 No Content
         * Standard response for successful DELETE operations
         * No body needed — just confirm it was deleted
         */
        return ResponseEntity.noContent().build();
    }
}