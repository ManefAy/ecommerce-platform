package com.manef.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Response sent to the frontend when returning
 * category data.
 *
 * Notice what we EXCLUDE compared to the Entity:
 * → No JPA annotations
 * → No lazy-loaded relationships that could cause
 *   infinite loops when converting to JSON
 *
 * Example response:
 * {
 *   "id": 1,
 *   "name": "Electronics",
 *   "slug": "electronics",
 *   "description": "All electronic products",
 *   "parentId": null,
 *   "parentName": null,
 *   "subCategories": [
 *     { "id": 2, "name": "Phones", "slug": "phones" },
 *     { "id": 3, "name": "Laptops", "slug": "laptops" }
 *   ]
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private Long id;

    private String name;

    private String slug;

    private String description;

    /**
     * Parent category ID — null if top-level category
     * Frontend uses this to build breadcrumbs:
     * Home → Electronics → Phones
     */
    private Long parentId;

    /**
     * Parent category name — null if top-level category
     * Sent together with parentId for convenience
     * so frontend does not need a second API call
     * just to get the parent name
     */
    private String parentName;

    /**
     * List of subcategories belonging to this category.
     * Only populated when fetching a single category.
     * Empty list when fetching all categories
     * to keep the response lightweight.
     */
    private List<CategoryResponse> subCategories;
}