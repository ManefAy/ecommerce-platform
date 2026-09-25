package com.manef.ecommerce.service;

import com.manef.ecommerce.dto.request.OrderRequest;
import com.manef.ecommerce.dto.response.OrderResponse;
import com.manef.ecommerce.entity.*;
import com.manef.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the entire order lifecycle:
 * → Place a new order (checkout flow)
 * → Get order by order number (tracking)
 * → Get all orders for a user (order history)
 * → Get all orders (admin)
 * → Update order status (admin)
 * → Update payment status (admin)
 * → Cancel an order (admin)
 *
 * The checkout flow when a customer places an order:
 * 1. Validate all products exist and have enough stock
 * 2. Calculate totals
 * 3. Save the order and order items to MySQL
 * 4. Decrease stock for each product
 * 5. Record stock movements
 * 6. Generate order number
 * 7. Return order confirmation
 *
 * Note: Delivery API and Meta CAPI calls
 * will be added in Phase 3.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StockMovementRepository stockMovementRepository;

    // ─────────────────────────────────────────────────────
    // PLACE ORDER — Main checkout flow
    // ─────────────────────────────────────────────────────

    /**
     * Places a new order.
     * This is the most critical method in the entire system.
     *
     * @param request → OrderRequest DTO from the frontend
     * @param userEmail → email of logged-in user, null for guests
     * @return → OrderResponse with order details and tracking info
     */
    @Transactional
    public OrderResponse placeOrder(OrderRequest request, String userEmail) {

        // ── Step 1: Load user if logged in ─────────────
        /**
         * userEmail is null for guest checkout
         * If provided → load the user from DB
         * If not → order will have user = null (guest)
         */
        User user = null;
        if (userEmail != null) {
            user = userRepository.findByEmail(userEmail)
                    .orElse(null);
        }

        // ── Step 2: Validate products and build order items
        /**
         * For each item in the request:
         * 1. Check the product exists
         * 2. Check there is enough stock
         * 3. Build the OrderItem entity
         * 4. Calculate subtotal
         */
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {

            // Load product from DB
            Product product = productRepository
                    .findById(itemRequest.getProductId())
                    .orElseThrow(() ->
                            new RuntimeException(
                                "Product not found with id: "
                                + itemRequest.getProductId()
                            )
                    );

            // Check product is active
            if (!product.getActive()) {
                throw new RuntimeException(
                    "Product is no longer available: " + product.getTitle()
                );
            }

            // Check enough stock is available
            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new RuntimeException(
                    "Insufficient stock for: " + product.getTitle()
                    + ". Available: " + product.getStockQuantity()
                    + ", Requested: " + itemRequest.getQuantity()
                );
            }

            /**
             * Use discountPrice if available, otherwise use regular price.
             * This is the price snapshot — frozen at checkout time.
             */
            BigDecimal unitPrice = product.getDiscountPrice() != null
                    ? product.getDiscountPrice()
                    : product.getPrice();

            // Calculate subtotal for this item
            BigDecimal subTotal = unitPrice.multiply(
                    BigDecimal.valueOf(itemRequest.getQuantity())
            );

            // Add to total
            totalAmount = totalAmount.add(subTotal);

            // Build OrderItem entity
            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .subTotal(subTotal)
                    .build();

            orderItems.add(orderItem);
        }

        // ── Step 3: Build and save the Order ───────────
        /**
         * Generate a unique order number.
         * Format: ORD-YYYYMMDD-XXXX
         * e.g. ORD-20240101-1234
         */
        String orderNumber = generateOrderNumber();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .user(user)
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .shippingAddress(request.getShippingAddress())
                .city(request.getCity())
                .notes(request.getNotes())
                .totalAmount(totalAmount)
                .shippingFee(BigDecimal.ZERO)
                .paymentMethod(Order.PaymentMethod.CASH_ON_DELIVERY)
                .paymentStatus(Order.PaymentStatus.UNPAID)
                .orderStatus(Order.OrderStatus.PROCESSING)
                .build();

        // Save the order first to get its ID
        Order savedOrder = orderRepository.save(order);

        // ── Step 4: Save order items ────────────────────
        /**
         * Now that we have the order ID,
         * link each order item to the order and save.
         */
        for (OrderItem item : orderItems) {
            item.setOrder(savedOrder);
            orderItemRepository.save(item);
        }

        // ── Step 5: Update stock and record movements ───
        /**
         * For each ordered product:
         * 1. Decrease stock quantity
         * 2. Record a SALE stock movement for audit trail
         */
        for (int i = 0; i < orderItems.size(); i++) {
            OrderItem item = orderItems.get(i);
            Product product = item.getProduct();

            int quantityBefore = product.getStockQuantity();
            int quantityAfter = quantityBefore - item.getQuantity();

            // Update product stock
            product.setStockQuantity(quantityAfter);
            productRepository.save(product);

            // Record stock movement
            StockMovement movement = StockMovement.builder()
                    .product(product)
                    .quantityChanged(-item.getQuantity())
                    .quantityBefore(quantityBefore)
                    .quantityAfter(quantityAfter)
                    .reason(StockMovement.MovementReason.SALE)
                    .note("Order " + orderNumber + " placed")
                    .build();

            stockMovementRepository.save(movement);
        }

        /**
         * TODO Phase 3:
         * → Call Delivery API to generate tracking number
         * → Call Meta Conversions API to send Purchase event
         */

        // ── Step 6: Return the order response ──────────
        return mapToResponse(savedOrder, orderItems);
    }

    // ─────────────────────────────────────────────────────
    // GET ORDERS
    // ─────────────────────────────────────────────────────

    /**
     * Get an order by its order number.
     * Used on the order tracking page.
     * Public — guest customers can track their order.
     */
    @Transactional
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() ->
                        new RuntimeException(
                            "Order not found: " + orderNumber
                        )
                );
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        return mapToResponse(order, items);
    }

    /**
     * Get all orders for a logged-in user.
     * Used on the customer's order history page.
     */
    @Transactional
    public List<OrderResponse> getMyOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new RuntimeException("User not found: " + userEmail)
                );

        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository
                            .findByOrderId(order.getId());
                    return mapToResponse(order, items);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all orders.
     * Used in admin dashboard order management.
     */
    @Transactional
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository
                            .findByOrderId(order.getId());
                    return mapToResponse(order, items);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get orders by status.
     * Used in admin dashboard to filter orders.
     */
    @Transactional
    public List<OrderResponse> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByOrderStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository
                            .findByOrderId(order.getId());
                    return mapToResponse(order, items);
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────
    // ADMIN — UPDATE ORDER
    // ─────────────────────────────────────────────────────

    /**
     * Update order status.
     * Used by admin to move order through the flow:
     * PROCESSING → SHIPPED → DELIVERED
     */
    @Transactional
    public OrderResponse updateOrderStatus(
            Long orderId,
            Order.OrderStatus newStatus) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found with id: " + orderId)
                );

        order.setOrderStatus(newStatus);
        Order updated = orderRepository.save(order);
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return mapToResponse(updated, items);
    }

    /**
     * Update payment status.
     * Used by admin when COD payment is confirmed on delivery.
     */
    @Transactional
    public OrderResponse updatePaymentStatus(
            Long orderId,
            Order.PaymentStatus newStatus) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found with id: " + orderId)
                );

        order.setPaymentStatus(newStatus);
        Order updated = orderRepository.save(order);
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return mapToResponse(updated, items);
    }

    /**
     * Cancel an order.
     * Restores stock for all items in the order.
     */
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found with id: " + orderId)
                );

        // Can only cancel PROCESSING orders
        if (order.getOrderStatus() != Order.OrderStatus.PROCESSING) {
            throw new RuntimeException(
                "Cannot cancel order with status: " + order.getOrderStatus()
                + ". Only PROCESSING orders can be canceled."
            );
        }

        // Restore stock for each item
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        for (OrderItem item : items) {
            Product product = item.getProduct();

            int quantityBefore = product.getStockQuantity();
            int quantityAfter = quantityBefore + item.getQuantity();

            // Restore stock
            product.setStockQuantity(quantityAfter);
            productRepository.save(product);

            // Record stock movement
            StockMovement movement = StockMovement.builder()
                    .product(product)
                    .quantityChanged(item.getQuantity())
                    .quantityBefore(quantityBefore)
                    .quantityAfter(quantityAfter)
                    .reason(StockMovement.MovementReason.CANCELED)
                    .note("Order " + order.getOrderNumber() + " canceled")
                    .build();

            stockMovementRepository.save(movement);
        }

        // Update order status
        order.setOrderStatus(Order.OrderStatus.CANCELED);
        Order updated = orderRepository.save(order);
        return mapToResponse(updated, items);
    }

    // ─────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────

    /**
     * Generate a unique order number.
     * Format: ORD-YYYYMMDD-XXXX
     * e.g. ORD-20240101-1234
     *
     * We use the current timestamp in milliseconds
     * at the end to ensure uniqueness.
     */
    private String generateOrderNumber() {
        String datePart = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        /**
         * Count all existing orders in DB and add 1.
         * This guarantees a unique sequential number.
         * e.g. 0 orders → "0001", 1 order → "0002"
         *
         * String.format("%04d") → formats with leading zeros
         * e.g. 1 → "0001", 23 → "0023", 100 → "0100"
         */
        long orderCount = orderRepository.count() + 1;
        String sequentialPart = String.format("%04d", orderCount);

        return "ORD-" + datePart + "-" + sequentialPart;
    }

    /**
     * Converts Order entity and its items into OrderResponse DTO.
     */
    private OrderResponse mapToResponse(Order order, List<OrderItem> items) {

        // Calculate grand total = totalAmount + shippingFee
        BigDecimal grandTotal = order.getTotalAmount()
                .add(order.getShippingFee());

        // Map order items to response DTOs
        List<OrderResponse.OrderItemResponse> itemResponses = items.stream()
                .map(item -> OrderResponse.OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productTitle(item.getProduct().getTitle())
                        // Get first image if exists
                        .productImage(
                            item.getProduct().getImages() != null &&
                            !item.getProduct().getImages().isEmpty()
                                ? item.getProduct().getImages().get(0).getImageUrl()
                                : null
                        )
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subTotal(item.getSubTotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .shippingAddress(order.getShippingAddress())
                .city(order.getCity())
                .notes(order.getNotes())
                .totalAmount(order.getTotalAmount())
                .shippingFee(order.getShippingFee())
                .grandTotal(grandTotal)
                .paymentMethod(order.getPaymentMethod().name())
                .paymentStatus(order.getPaymentStatus().name())
                .orderStatus(order.getOrderStatus().name())
                .trackingNumber(order.getTrackingNumber())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}