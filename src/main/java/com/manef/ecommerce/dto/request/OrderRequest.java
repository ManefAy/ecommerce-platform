package com.manef.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.util.List;

/**
 * Data sent by the CUSTOMER when placing an order.
 * This is the most important request DTO —
 * it triggers the entire order flow:
 *
 * 1. Save order in MySQL
 * 2. Call Delivery API to generate tracking number
 * 3. Send event to Meta Conversions API
 * 4. Return order confirmation to customer
 */
@Data
public class OrderRequest {

    /**
     * Customer's full name
     * Required for delivery label generation
     */
    @NotBlank(message = "Customer name is required")
    private String customerName;

    /**
     * Customer's phone number
     * Required by Delivery API to contact
     * customer during delivery
     */
    @NotBlank(message = "Phone number is required")
    private String customerPhone;

    /**
     * Full street address
     * e.g. "123 Rue de la Paix, Apt 4B"
     */
    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    /**
     * City of delivery
     * Stored separately for Delivery API
     * and admin filtering
     */
    @NotBlank(message = "City is required")
    private String city;

    /**
     * Optional notes from the customer
     * e.g. "Please call before delivery"
     *      "Leave at the door"
     */
    private String notes;

    /**
     * List of items the customer is ordering.
     * @NotEmpty → order must have at least one item
     * An empty cart should never reach the backend
     * but we validate it here as a safety net
     */
    @NotEmpty(message = "Order must contain at least one item")
    private List<OrderItemRequest> items;

    // ─────────────────────────────────────────────────────
    // NESTED DTO
    // ─────────────────────────────────────────────────────

    /**
     * Represents a single item inside the order request.
     * Nested inside OrderRequest because it has no
     * meaning outside of an order context.
     *
     * The customer sends:
     * {
     *   "customerName": "Ayouni",
     *   "customerPhone": "12345678",
     *   "shippingAddress": "123 Rue ...",
     *   "city": "Tunis",
     *   "items": [
     *     { "productId": 1, "quantity": 2 },
     *     { "productId": 5, "quantity": 1 }
     *   ]
     * }
     *
     * We fetch the actual price from the DB in the
     * service layer — customers cannot send their
     * own price (security measure)
     */
    @Data
    public static class OrderItemRequest {

        /**
         * Which product the customer wants
         */
        @NotNull(message = "Product ID is required")
        private Long productId;

        /**
         * How many units they want
         * @Positive → must be at least 1
         */
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be at least 1")
        private Integer quantity;
    }
}