package com.manef.ecommerce.service;

import com.manef.ecommerce.dto.request.DeliveryRequest;
import com.manef.ecommerce.dto.response.DeliveryResponse;
import com.manef.ecommerce.dto.request.OrderRequest;
import com.manef.ecommerce.dto.response.OrderResponse;
import com.manef.ecommerce.entity.*;
import com.manef.ecommerce.exception.BusinessException;
import com.manef.ecommerce.exception.ResourceNotFoundException;
import com.manef.ecommerce.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
	
    // Used to build the random part of order numbers
    private static final String ORDER_CODE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StockMovementRepository stockMovementRepository;
    private final DeliveryApiService deliveryApiService;
    private final MetaCapiService metaCapiService;
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

        for (OrderRequest.OrderItemRequest itemRequest : mergeItems(request.getItems())) {

            // Load product from DB and lock its row until the order is saved
            Product product = productRepository
                    .findByIdForUpdate(itemRequest.getProductId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                "Product not found with id: "
                                + itemRequest.getProductId()
                            )
                    );

            // Check product is active
            if (!product.getActive()) {
                throw new BusinessException(
                    "Product is no longer available: " + product.getTitle()
                );
            }

            // Check enough stock is available
            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new BusinessException(
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

     // ── Step 6: Call Delivery API ──────────────────
        /**
         * Build our standard delivery request
         * from the order data.
         *
         * weightKg → sum of all item weights
         * We calculate it by multiplying each product's
         * weight by its quantity and adding them up.
         */
        BigDecimal totalWeight = orderItems.stream()
                .map(item -> item.getProduct().getWeight()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        DeliveryRequest deliveryRequest = DeliveryRequest.builder()
                .orderNumber(savedOrder.getOrderNumber())
                .customerName(savedOrder.getCustomerName())
                .customerPhone(savedOrder.getCustomerPhone())
                .shippingAddress(savedOrder.getShippingAddress())
                .city(savedOrder.getCity())
                .weightKg(totalWeight)
                .codAmount(savedOrder.getTotalAmount())
                .notes(savedOrder.getNotes())
                .build();

        DeliveryResponse deliveryResponse =
                deliveryApiService.createShipment(deliveryRequest);

        /**
         * Update the order with delivery API response data.
         * Even if the delivery API failed → we still save
         * what we have and the admin can fix it manually.
         */
        if (deliveryResponse.isSuccess()) {
            savedOrder.setTrackingNumber(deliveryResponse.getTrackingNumber());
            savedOrder.setShippingFee(deliveryResponse.getShippingFee());
            savedOrder.setDeliveryApiResponse(deliveryResponse.getRawResponse());
        } else {
            /**
             * Delivery API failed → log it but don't fail the order.
             * Store the error in deliveryApiResponse for admin to see.
             * Admin can manually create the shipment later.
             */
            savedOrder.setDeliveryApiResponse(
                    "FAILED: " + deliveryResponse.getErrorMessage()
            );
        }

        // Save the updated order with tracking info
        savedOrder = orderRepository.save(savedOrder);

        // ── Step 7: Send Meta CAPI event (async) ───────
        /**
         * This runs in a background thread — customer
         * doesn't wait for this to complete.
         * If Meta CAPI fails → order still completes.
         */
        metaCapiService.sendPurchaseEvent(savedOrder, orderItems);

        // ── Step 8: Return the order response ──────────
        return mapToResponse(savedOrder, orderItems);
    }

    // ─────────────────────────────────────────────────────
    // GET ORDERS
    // ─────────────────────────────────────────────────────

    /**
     * Get an order by its order number.
     * Used on the order tracking page.
     * Public — guest customers can track their order.
     *
     * The customer must also give the phone number used
     * at checkout. Without it, anyone guessing an order
     * number could read the customer's name, phone and address.
     * A wrong phone returns the same "not found" error as a
     * wrong order number, so it doesn't reveal which orders exist.
     */
    @Transactional
    public OrderResponse getOrderByOrderNumber(String orderNumber, String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BusinessException("Phone number is required");
        }

        Order order = orderRepository.findByOrderNumber(orderNumber)
                .filter(o -> samePhone(o.getCustomerPhone(), phone))
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                            "Order not found: " + orderNumber
                        )
                );
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        return mapToResponse(order, items);
    }

    /**
     * Compares two phone numbers ignoring spaces, dashes
     * and the +216 / 00216 country code.
     * e.g. "+216 12 345 678" and "12345678" match.
     */
    private boolean samePhone(String stored, String given) {
        String a = stored == null ? "" : stored.replaceAll("\\D", "");
        String b = given.replaceAll("\\D", "");
        if (a.isEmpty() || b.isEmpty()) return false;
        String lastA = a.length() > 8 ? a.substring(a.length() - 8) : a;
        String lastB = b.length() > 8 ? b.substring(b.length() - 8) : b;
        return lastA.equals(lastB);
    }

    /**
     * Get all orders for a logged-in user.
     * Used on the customer's order history page.
     */
    @Transactional
    public List<OrderResponse> getMyOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found: " + userEmail)
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

        // Canceling must restore stock → always go through cancelOrder()
        if (newStatus == Order.OrderStatus.CANCELED) {
            return cancelOrder(orderId);
        }

        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with id: " + orderId)
                );

        // Only allow the documented flow: PROCESSING → SHIPPED → DELIVERED
        if (!order.getOrderStatus().canTransitionTo(newStatus)) {
            throw new BusinessException(
                "Cannot change order status from " + order.getOrderStatus()
                + " to " + newStatus
            );
        }

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
                        new ResourceNotFoundException("Order not found with id: " + orderId)
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

        // Lock the order row so two cancel requests can't both restore stock
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found with id: " + orderId)
                );

        // Can only cancel PROCESSING orders
        if (!order.getOrderStatus().canTransitionTo(Order.OrderStatus.CANCELED)) {
            throw new BusinessException(
                "Cannot cancel order with status: " + order.getOrderStatus()
                + ". Only PROCESSING orders can be canceled."
            );
        }

        // Restore stock for each item (products locked in id order to avoid deadlocks)
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        List<OrderItem> itemsByProduct = new ArrayList<>(items);
        itemsByProduct.sort(Comparator.comparing(item -> item.getProduct().getId()));

        for (OrderItem item : itemsByProduct) {
            Product product = productRepository
                    .findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                "Product not found with id: " + item.getProduct().getId()
                            )
                    );

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
     * Combines lines for the same product into one line
     * (e.g. product 5 × 3 and product 5 × 3 → product 5 × 6),
     * so the stock check sees the real total quantity.
     * TreeMap keeps products sorted by id → rows are always
     * locked in the same order, which avoids deadlocks.
     */
    private List<OrderRequest.OrderItemRequest> mergeItems(
            List<OrderRequest.OrderItemRequest> items) {
        Map<Long, Integer> quantities = new TreeMap<>();
        for (OrderRequest.OrderItemRequest item : items) {
            quantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        List<OrderRequest.OrderItemRequest> merged = new ArrayList<>();
        quantities.forEach((productId, quantity) -> {
            OrderRequest.OrderItemRequest line = new OrderRequest.OrderItemRequest();
            line.setProductId(productId);
            line.setQuantity(quantity);
            merged.add(line);
        });
        return merged;
    }

    /**
     * Generate a unique order number.
     * Format: ORD-YYYYMMDD-XXXXXX
     * e.g. ORD-20261018-K7P2QX
     *
     * The last part is 6 random characters, so order numbers
     * can't be guessed by counting up (the old format used
     * the order count: -0001, -0002, ...).
     * The alphabet skips 0/O and 1/I/L so customers can't
     * misread their number. 31^6 ≈ 887 million codes per day.
     */
    private String generateOrderNumber() {
        String datePart = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        String orderNumber;
        do {
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                code.append(ORDER_CODE_ALPHABET.charAt(
                        RANDOM.nextInt(ORDER_CODE_ALPHABET.length())));
            }
            orderNumber = "ORD-" + datePart + "-" + code;
        } while (orderRepository.existsByOrderNumber(orderNumber));

        return orderNumber;
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