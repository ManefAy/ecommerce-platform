package com.manef.ecommerce.repository;

import com.manef.ecommerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Get all items belonging to a specific order.
     * Used when displaying order details.
     *
     * Generated SQL:
     * SELECT * FROM order_items WHERE order_id = ?
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Get all order items for a specific product.
     * Used in admin dashboard to see how many times
     * a product has been ordered.
     *
     * Generated SQL:
     * SELECT * FROM order_items WHERE product_id = ?
     */
    List<OrderItem> findByProductId(Long productId);
}