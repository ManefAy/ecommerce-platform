package com.manef.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data sent by the ADMIN when creating or updating a category.
 * This DTO is used for both CREATE and UPDATE operations.
 *
 * Why use the same DTO for both?
 * → Less code to maintain
 * → In the service layer we check:
 *    if id exists → UPDATE
 *    if id is null → CREATE
 */
@Data
public class CategoryRequest {

    /**
     * Display name of the category
     * e.g. "Electronics", "Men's Clothing"
     */
    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name cannot exceed 100 characters")
    private String name;

    /**
     * URL-friendly slug
     * e.g. "electronics", "mens-clothing"
     * If admin does not provide this, we will
     * auto-generate it from the name in the service layer.
     * e.g. "Men's Clothing" → "mens-clothing"
     */
    @NotBlank(message = "Slug is required")
    @Size(max = 100, message = "Slug cannot exceed 100 characters")
    private String slug;

    /**
     * Optional description for the category page
     */
    private String description;

    /**
     * Optional parent category ID for subcategories.
     * NULL → this is a top-level category
     * 1L   → this is a subcategory of category with id=1
     */
    private Long parentId;
}