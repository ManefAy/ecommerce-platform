package com.manef.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response sent to the frontend when returning
 * product data.
 *
 * Used in:
 * → Product listing page (all products)
 * → Product detail page (single product)
 * → Admin dashboard product management
 *
 * Example response:
 * {
 *   "id": 1,
 *   "title": "Nike Air Max 2024",
 *   "price": 99.99,
 *   "discountPrice": 79.99,
 *   "stockQuantity": 50,
 *   "slug": "nike-air-max-2024",
 *   "active": true,
 *   "categoryId": 2,
 *   "categoryName": "Shoes",
 *   "images": [
 *     { "id": 1, "imageUrl": "uploads/nike-front.jpg", "displayOrder": 1 },
 *     { "id": 2, "imageUrl": "uploads/nike-side.jpg",  "displayOrder": 2 }
 *   ]
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;

    private String title;

    private String description;

    private BigDecimal price;

    /**
     * Sale price — null if product is not on sale
     * Frontend logic:
     * if discountPrice != null → show discountPrice in red
     *                            show price crossed out
     * if discountPrice == null → show price normally
     */
    private BigDecimal discountPrice;

    private Integer stockQuantity;

    private BigDecimal weight;

    private String slug;

    private Boolean active;

    /**
     * Category ID and name sent together
     * so frontend does not need a second API call
     * just to display the category name
     */
    private Long categoryId;

    private String categoryName;

    /**
     * All images for this product
     * ordered by displayOrder ascending
     * First image (displayOrder=1) is the thumbnail
     */
    private List<ImageResponse> images;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ─────────────────────────────────────────────────────
    // NESTED DTO
    // ─────────────────────────────────────────────────────

    /**
     * Represents a single product image in the response.
     * Nested inside ProductResponse because it has no
     * meaning outside of a product context.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageResponse {

        private Long id;

        /**
         * Full URL or relative path of the image
         * Frontend uses this directly in <img src="...">
         */
        private String imageUrl;

        /**
         * Display order — frontend renders images
         * in this order in the product gallery
         */
        private Integer displayOrder;
    }
}