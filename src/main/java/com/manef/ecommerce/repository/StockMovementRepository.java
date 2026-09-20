package com.manef.ecommerce.repository;

import com.manef.ecommerce.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    /**
     * Get all stock movements for a specific product.
     * Ordered by newest first.
     * Used in admin dashboard to show full stock history
     * of a product.
     *
     * Generated SQL:
     * SELECT * FROM stock_movements
     * WHERE product_id = ?
     * ORDER BY created_at DESC
     */
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    /**
     * Get all stock movements by reason type.
     * Used in admin dashboard to filter movements.
     * e.g. show all RESTOCK movements only
     *
     * Generated SQL:
     * SELECT * FROM stock_movements
     * WHERE reason = ?
     * ORDER BY created_at DESC
     */
    List<StockMovement> findByReasonOrderByCreatedAtDesc(
        StockMovement.MovementReason reason
    );
}