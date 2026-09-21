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
 * order data.
 *
 * Used in:
 * → Thank you page after checkout
 * → Customer order history page
 * → Order tracking page
 * → Admin dashboard order management
 *
 * Example response:
 * {
 *   "id": 1,
 *   "orderNumber": "ORD-2024-001",
 *   "customerName": "Ayouni",
 *   "customerPhone": "12345678",
 *   "shippingAddress": "123 Rue de la Paix",
 *   "city": "Tunis",
 *   "totalAmount": 199.98,
 *   "shippingFee": 7.00,
 *   "paymentMethod": "CASH_ON_DELIVERY",
 *   "paymentStatus": "UNPAID",
 *   "orderStatus": "PROCESSING",
 *   "trackingNumber": "TRK123456",
 *   "items": [...],
 *   "createdAt": "2024-01-01T10:00:00"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long id;

    /**
     * Human-readable order number
     * Shown to customer on thank you page
     * e.g. "ORD-2024-001"
     */
    private String orderNumber;

    private String customerName;

    private String customerPhone;

    private String shippingAddress;

    private String city;

    private String notes;

    /**
     * Sum of all order items
     */
    private BigDecimal totalAmount;

    /**
     * Shipping fee from Delivery API
     * Shown separately so customer sees:
     * Subtotal:  199.98
     * Shipping:  +7.00
     * Total:     206.98
     */
    private BigDecimal shippingFee;

    /**
     * Grand total = totalAmount + shippingFee
     * Calculated in service layer
     */
    private BigDecimal grandTotal;

    /**
     * CASH_ON_DELIVERY or CARD
     */
    private String paymentMethod;

    /**
     * UNPAID or PAID
     */
    private String paymentStatus;

    /**
     * PROCESSING, SHIPPED, DELIVERED or CANCELED
     */
    private String orderStatus;

    /**
     * Tracking number from Delivery API
     * Shown on thank you page so customer
     * can track their package immediately
     */
    private String trackingNumber;

    /**
     * All items inside this order
     */
    private List<OrderItemResponse> items;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // ─────────────────────────────────────────────────────
    // NESTED DTO
    // ─────────────────────────────────────────────────────

    /**
     * Represents a single item inside the order response.
     * Contains product details for display purposes
     * in the order summary.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {

        private Long id;

        /**
         * Product details — included so frontend
         * can show product title and image
         * in the order summary without extra API calls
         */
        private Long productId;

        private String productTitle;

        /**
         * First image of the product (displayOrder=1)
         * shown as thumbnail in order summary
         */
        private String productImage;

        private Integer quantity;

        /**
         * Price at time of purchase — frozen snapshot
         */
        private BigDecimal unitPrice;

        /**
         * quantity × unitPrice
         */
        private BigDecimal subTotal;
    }
}