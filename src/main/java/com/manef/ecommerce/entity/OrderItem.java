package com.manef.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Represents a single product line inside an order.
 *
 * Example: if a customer orders 2 Nike shoes and 1 Samsung phone,
 * that creates ONE Order with TWO OrderItem rows:
 *
 * order_id=1, product_id=5, quantity=2, unitPrice=99.99
 * order_id=1, product_id=8, quantity=1, unitPrice=799.99
 *
 * Key design decision:
 * We store unitPrice HERE on the OrderItem, NOT just a reference
 * to the product price. Why?
 * → Product prices can change in the future.
 * → We need to remember what the customer ACTUALLY paid at
 *   the time of the order, not the current price.
 * → e.g. Customer bought Nike shoes for 99.99 in January.
 *         Admin changed price to 129.99 in March.
 *         The January order must still show 99.99.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * How many units of this product the customer ordered
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * The price of ONE unit at the time of purchase.
     * This is a snapshot — frozen at checkout time.
     * If discountPrice was active, we store that here.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Convenience field: quantity × unitPrice
     * e.g. 2 × 99.99 = 199.98
     * Stored in DB so we don't recalculate it every time
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subTotal;

    // ─────────────────────────────────────────────────────
    // RELATIONSHIPS
    // ─────────────────────────────────────────────────────

    /**
     * Which order this item belongs to.
     *
     * @ManyToOne → Many items belong to ONE order
     * @JoinColumn → FK column in this table is "order_id"
     * nullable = false → an item must always belong to an order
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Which product this item refers to.
     *
     * @ManyToOne → Many order items can refer to ONE product
     * @JoinColumn → FK column in this table is "product_id"
     * nullable = false → an item must always have a product
     *
     * Note: we keep this reference for convenience
     * (e.g. showing product image in order history)
     * but the real price data is in unitPrice above
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}