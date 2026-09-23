package com.manef.ecommerce.service;

import com.manef.ecommerce.dto.request.CategoryRequest;
import com.manef.ecommerce.dto.response.CategoryResponse;
import com.manef.ecommerce.entity.Category;
import com.manef.ecommerce.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles all category business logic:
 * → Get all categories
 * → Get single category by ID or slug
 * → Create category (admin)
 * → Update category (admin)
 * → Delete category (admin)
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // ─────────────────────────────────────────────────────
    // GET ALL CATEGORIES
    // ─────────────────────────────────────────────────────

    /**
     * Returns all top-level categories with their subcategories.
     * Used in the storefront navigation menu.
     *
     * @return → list of CategoryResponse DTOs
     */
    public List<CategoryResponse> getAllCategories() {
        /**
         * findByParentIsNull() → only fetch top-level categories
         * Each category already has its subCategories loaded
         * because of CascadeType.ALL in the entity
         */
        return categoryRepository.findByParentIsNull()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────
    // GET SINGLE CATEGORY
    // ─────────────────────────────────────────────────────

    /**
     * Get a single category by its ID.
     * Used in admin dashboard edit form.
     */
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Category not found with id: " + id)
                );
        return mapToResponse(category);
    }

    /**
     * Get a single category by its slug.
     * Used in Next.js: /category/electronics
     */
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() ->
                        new RuntimeException("Category not found with slug: " + slug)
                );
        return mapToResponse(category);
    }

    // ─────────────────────────────────────────────────────
    // CREATE CATEGORY (ADMIN)
    // ─────────────────────────────────────────────────────

    /**
     * Creates a new category.
     * Only accessible by ROLE_ADMIN.
     *
     * @param request → CategoryRequest DTO from admin
     * @return        → the created CategoryResponse
     */
    public CategoryResponse createCategory(CategoryRequest request) {

        // Check slug is not already taken
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new RuntimeException(
                "Slug already exists: " + request.getSlug()
            );
        }

        // Build the Category entity
        Category category = Category.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .build();

        /**
         * If parentId is provided → this is a subcategory
         * Load the parent and set it on the entity
         */
        if (request.getParentId() != null) {
            Category parent = categoryRepository
                    .findById(request.getParentId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                "Parent category not found with id: "
                                + request.getParentId()
                            )
                    );
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        return mapToResponse(saved);
    }

    // ─────────────────────────────────────────────────────
    // UPDATE CATEGORY (ADMIN)
    // ─────────────────────────────────────────────────────

    /**
     * Updates an existing category.
     * Only accessible by ROLE_ADMIN.
     *
     * @param id      → the category ID to update
     * @param request → new data from admin
     * @return        → updated CategoryResponse
     */
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {

        // Find the existing category
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Category not found with id: " + id)
                );

        /**
         * Check slug uniqueness only if the slug changed.
         * If admin kept the same slug → no need to check.
         */
        if (!category.getSlug().equals(request.getSlug()) &&
             categoryRepository.existsBySlug(request.getSlug())) {
            throw new RuntimeException(
                "Slug already exists: " + request.getSlug()
            );
        }

        // Update the fields
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());

        // Update parent if provided
        if (request.getParentId() != null) {
            Category parent = categoryRepository
                    .findById(request.getParentId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                "Parent category not found with id: "
                                + request.getParentId()
                            )
                    );
            category.setParent(parent);
        } else {
            // If parentId is null → make it a top-level category
            category.setParent(null);
        }

        Category updated = categoryRepository.save(category);
        return mapToResponse(updated);
    }

    // ─────────────────────────────────────────────────────
    // DELETE CATEGORY (ADMIN)
    // ─────────────────────────────────────────────────────

    /**
     * Deletes a category by ID.
     * CascadeType.ALL in the entity means subcategories
     * are also deleted automatically.
     * Only accessible by ROLE_ADMIN.
     *
     * @param id → the category ID to delete
     */
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Category not found with id: " + id)
                );
        categoryRepository.delete(category);
    }

    // ─────────────────────────────────────────────────────
    // MAPPER — Entity to DTO
    // ─────────────────────────────────────────────────────

    /**
     * Converts a Category entity into a CategoryResponse DTO.
     * This keeps our entity layer separate from the API layer.
     *
     * Called every time we need to return category data
     * to the frontend.
     */
    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                // parentId and parentName — null if top-level
                .parentId(category.getParent() != null
                        ? category.getParent().getId()
                        : null)
                .parentName(category.getParent() != null
                        ? category.getParent().getName()
                        : null)
                // Map subcategories recursively
                .subCategories(category.getSubCategories() != null
                        ? category.getSubCategories()
                                .stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList())
                        : null)
                .build();
    }
}