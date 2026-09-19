package com.manef.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a customer order in the system.
 *
 * Key design decisions:
 *
 * 1. user_id is NULLABLE → supports guest checkout
 *    Customers can order without creating an account
 *
 * 2. customerName and customerPhone are stored directly
 *    on the order → even if the user deletes their account,
 *    the order data is preserved
 *
 * 3. Three separate status fields:
 *    → orderStatus   : where is the order in the process?
 *    → paymentStatus : has the customer paid?
 *    → paymentMethod : how will/did they pay?
 *
 * 4. trackingNumber → received from the Delivery API
 *    after we submit the order to them automatically
 */

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Order {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	/**
     * Human-readable unique order identifier
     * shown to the customer on the thank you page
     * e.g. "ORD-2024-00123"
     * We will generate this in the service layer (Phase 3)
     */
	@Column(nullable=false, unique=true, length=50)
	private String orderNumber;
	
	/**
     * The registered user who placed this order.
     *
     * @ManyToOne → Many orders can belong to ONE user
     * nullable = true → NULL means it was a guest checkout
     *
     * FetchType.LAZY → don't load the full User object
     * every time we load an order
     */
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="user_id", nullable=true)
	private User user;
	
	/**
     * Customer's full name — stored directly on order
     * because guest customers have no User record
     */
	@Column(nullable=false, length=100)
	private String customerName;
	
	 /**
     * Customer's phone number
     * Required by the Delivery API to contact
     * the customer for delivery coordination
     */
	@Column(nullable=false, length=20)
	private String customerPhone;
	
	/**
     * Full street address for delivery
     * e.g. "123 Rue de la Paix, Apt 4B"
     */
	@Column(nullable=false, length=255)
	private String shippingAddress;
	
	/**
     * City of delivery
     * Stored separately because:
     * → Delivery API needs city as a separate field
     * → Admin can filter orders by city
     * → Analytics: which cities order the most?
     */
	@Column(nullable=false, length=100)
	private String city;
	
	/**
     * Total order amount (sum of all order items)
     * Does NOT include shipping fee — that's separate
     */
	@Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
	
	/**
     * Shipping fee returned by the Delivery API
     * Stored separately so we can show it to the customer
     * and calculate the grand total correctly
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    /**
     * Optional notes from the customer
     * e.g. "Please leave at the door"
     *      "Call before delivery"
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * How the customer will pay / has paid
     * CASH_ON_DELIVERY → default, pay when package arrives
     * CARD             → paid online via Stripe/PayPal
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    /**
     * Whether the order has been paid
     * UNPAID → default for COD orders
     * PAID   → set after online payment or COD confirmation
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    /**
     * Where the order is in the fulfillment process
     * PROCESSING → just placed, being prepared
     * SHIPPED    → handed to delivery company
     * DELIVERED  → customer received the package
     * CANCELED   → order was canceled
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus orderStatus;

    /**
     * Tracking number received from the Delivery API
     * after we automatically submit the order to them.
     * Shown to the customer on the thank you page.
     * nullable → we get this AFTER the order is created
     */
    @Column(length = 100)
    private String trackingNumber;

    /**
     * Raw JSON response from the Delivery API
     * We store the full response as a safety net —
     * if their tracking system goes down, we still
     * have all the data we need locally
     * columnDefinition = "TEXT" → can store large JSON strings
     */
    @Column(columnDefinition = "TEXT")
    private String deliveryApiResponse;

    // ─────────────────────────────────────────────────────
    // RELATIONSHIPS
    // ─────────────────────────────────────────────────────

    /**
     * All items inside this order
     *
     * @OneToMany → One order contains MANY items
     * mappedBy = "order" → OrderItem owns the relationship
     * CascadeType.ALL → saving order also saves its items
     * orphanRemoval → removing item from list deletes it from DB
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    // ─────────────────────────────────────────────────────
    // AUDIT TIMESTAMPS
    // ─────────────────────────────────────────────────────

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Updated every time the order status changes
     * e.g. PROCESSING → SHIPPED → DELIVERED
     * Useful for admin dashboard audit trail
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ─────────────────────────────────────────────────────
    // ENUMS
    // ─────────────────────────────────────────────────────

    public enum PaymentMethod {
        CASH_ON_DELIVERY,
        CARD
    }

    public enum PaymentStatus {
        UNPAID,
        PAID
    }

    public enum OrderStatus {
        PROCESSING,
        SHIPPED,
        DELIVERED,
        CANCELED
    }
}
