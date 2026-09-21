package com.manef.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Data sent by the ADMIN when creating or updating a product.
 *
 * Validation annotations used here:
 * @NotBlank  → String cannot be null or empty
 * @NotNull   → Object/Number cannot be null
 * @Positive  → Number must be greater than 0
 * @Size      → String length boundaries
 */
@Data
public class ProductRequest {

    /**
     * Product title
     * e.g. "Nike Air Max 2024 — White/Black"
     */
    @NotBlank(message = "Product title is required")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    /**
     * Full product description
     */
    private String description;

    /**
     * Regular selling price
     * @NotNull  → must be provided
     * @Positive → must be greater than 0
     *             e.g. -10.00 is rejected automatically
     */
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    private BigDecimal price;

    /**
     * Optional discount/sale price
     * No @NotNull → admin can leave this empty
     * @Positive  → if provided, must be greater than 0
     */
    @Positive(message = "Discount price must be greater than 0")
    private BigDecimal discountPrice;

    /**
     * Stock quantity available
     * @NotNull  → must be provided
     * @Positive → must be greater than 0
     */
    @NotNull(message = "Stock quantity is required")
    @Positive(message = "Stock quantity must be greater than 0")
    private Integer stockQuantity;

    /**
     * Product weight in KG
     * Required by the Delivery API
     * @NotNull  → must be provided
     * @Positive → must be greater than 0
     */
    @NotNull(message = "Weight is required")
    @Positive(message = "Weight must be greater than 0")
    private BigDecimal weight;

    /**
     * SEO-friendly URL slug
     * e.g. "nike-air-max-2024-white-black"
     * If not provided, auto-generated from title
     * in the service layer
     */
    @NotBlank(message = "Slug is required")
    @Size(max = 200, message = "Slug cannot exceed 200 characters")
    private String slug;

    /**
     * Whether the product is visible to customers
     * true  → visible in storefront
     * false → hidden from storefront
     */
    @NotNull(message = "Active status is required")
    private Boolean active;

    /**
     * Which category this product belongs to
     * Admin must select a category when creating a product
     */
    @NotNull(message = "Category is required")
    private Long categoryId;
}