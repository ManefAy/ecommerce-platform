package com.manef.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * Tracks every change in product stock quantity.
 * This is an AUDIT TABLE — it never gets updated, only inserted.
 *
 * Every time stock changes, we add a new row here explaining:
 * → Which product changed?
 * → By how much? (positive = added, negative = removed)
 * → Why did it change?
 * → When did it happen?
 *
 * Example rows in DB:
 *
 * product_id=1, change=-2, reason=SALE,          note="Order ORD-001"
 * product_id=1, change=+50, reason=RESTOCK,       note="Supplier delivery"
 * product_id=1, change=-1, reason=MANUAL_ADJUST,  note="Damaged item removed"
 * product_id=1, change=+2, reason=CANCELED,       note="Order ORD-005 canceled"
 * product_id=1, change=+1, reason=RETURN,         note="Customer return ORD-003"
 *
 * This gives the admin a complete history of stock movements
 * and makes it easy to debug inventory discrepancies.
 */
@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Which product's stock changed.
     *
     * @ManyToOne → Many stock movements can relate to ONE product
     * nullable = false → every movement must have a product
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * How much the stock changed.
     * POSITIVE number → stock increased  e.g. +50 (restock)
     * NEGATIVE number → stock decreased  e.g. -2  (sale)
     *
     * e.g. if stockQuantity was 100 and we sold 2:
     * quantityChanged = -2
     * new stockQuantity = 98
     */
    @Column(nullable = false)
    private Integer quantityChanged;

    /**
     * Stock quantity BEFORE this movement happened.
     * Stored for audit purposes — admin can verify:
     * "stock was 100, we sold 2, now it should be 98"
     */
    @Column(nullable = false)
    private Integer quantityBefore;

    /**
     * Stock quantity AFTER this movement happened.
     * Should equal: quantityBefore + quantityChanged
     */
    @Column(nullable = false)
    private Integer quantityAfter;

    /**
     * Why did the stock change?
     * SALE          → customer placed an order        (negative change)
     * RESTOCK       → admin added new stock           (positive change)
     * MANUAL_ADJUST → admin manually corrected stock  (positive or negative)
     * RETURN        → customer returned an item       (positive change)
     * CANCELED      → order was canceled              (positive change)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementReason reason;

    /**
     * Optional human-readable note explaining the movement.
     * e.g. "Order ORD-001 placed"
     *      "Supplier batch #4521 received"
     *      "Item found damaged during inspection"
     */
    @Column(length = 255)
    private String note;

    /**
     * Automatically set when this record is created.
     * updatable = false → audit records never change.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ─────────────────────────────────────────────────────
    // ENUM
    // ─────────────────────────────────────────────────────

    public enum MovementReason {
        SALE,
        RESTOCK,
        MANUAL_ADJUST,
        RETURN,
        CANCELED
    }
}