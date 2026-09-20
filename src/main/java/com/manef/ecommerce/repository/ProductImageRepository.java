package com.manef.ecommerce.repository;

import com.manef.ecommerce.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    /**
     * Get all images for a specific product
     * ordered by displayOrder ascending.
     * This ensures the main image (displayOrder=1)
     * always comes first.
     *
     * Generated SQL:
     * SELECT * FROM product_images
     * WHERE product_id = ?
     * ORDER BY display_order ASC
     */
    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(Long productId);

    /**
     * Delete all images belonging to a product.
     * Used when admin deletes a product or
     * replaces all its images at once.
     *
     * Generated SQL:
     * DELETE FROM product_images WHERE product_id = ?
     */
    void deleteByProductId(Long productId);
}