package com.manef.ecommerce.repository;

import com.manef.ecommerce.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find an order by its human-readable order number.
     * Used in the order tracking page.
     * e.g. customer enters "ORD-001" to track their order
     *
     * Generated SQL:
     * SELECT * FROM orders WHERE order_number = ?
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Get all orders placed by a specific registered user.
     * Used in the customer's order history page.
     * Ordered by newest first.
     *
     * Generated SQL:
     * SELECT * FROM orders WHERE user_id = ?
     * ORDER BY created_at DESC
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Get all orders with a specific status.
     * Used in admin dashboard to filter orders.
     * e.g. show all PROCESSING orders
     *
     * Generated SQL:
     * SELECT * FROM orders WHERE order_status = ?
     * ORDER BY created_at DESC
     */
    List<Order> findByOrderStatusOrderByCreatedAtDesc(Order.OrderStatus orderStatus);

    /**
     * Get all orders from a specific city.
     * Used in admin dashboard for city-based filtering.
     *
     * Generated SQL:
     * SELECT * FROM orders WHERE city = ?
     * ORDER BY created_at DESC
     */
    List<Order> findByCityOrderByCreatedAtDesc(String city);

    /**
     * Get all orders placed between two dates.
     * Used in admin dashboard analytics.
     * e.g. show all orders from this month
     *
     * Generated SQL:
     * SELECT * FROM orders
     * WHERE created_at BETWEEN ? AND ?
     * ORDER BY created_at DESC
     */
    List<Order> findByCreatedAtBetweenOrderByCreatedAtDesc(
        LocalDateTime start,
        LocalDateTime end
    );

    /**
     * Count total orders by status.
     * Used in admin dashboard summary cards.
     * e.g. "23 orders PROCESSING"
     *
     * Generated SQL:
     * SELECT COUNT(*) FROM orders WHERE order_status = ?
     */
    long countByOrderStatus(Order.OrderStatus orderStatus);

    /**
     * Calculate total revenue from all PAID orders.
     * Used in admin dashboard analytics.
     *
     * @Query → custom JPQL because we need SUM()
     * SUM(o.totalAmount) → adds up all order totals
     * paymentStatus = PAID → only count paid orders
     */
    @Query("SELECT SUM(o.totalAmount) FROM Order o " +
           "WHERE o.paymentStatus = 'PAID'")
    Double calculateTotalRevenue();

    /**
     * Calculate total revenue between two dates.
     * Used in admin dashboard for period analytics.
     * e.g. revenue this month vs last month
     */
    @Query("SELECT SUM(o.totalAmount) FROM Order o " +
           "WHERE o.paymentStatus = 'PAID' " +
           "AND o.createdAt BETWEEN :start AND :end")
    Double calculateRevenueBetween(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
}