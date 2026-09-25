package com.manef.ecommerce.controller;

import com.manef.ecommerce.dto.request.OrderRequest;
import com.manef.ecommerce.dto.response.OrderResponse;
import com.manef.ecommerce.entity.Order;
import com.manef.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for order endpoints.
 *
 * Public routes:
 * → POST /api/orders              → place a new order
 * → GET  /api/orders/track/{orderNumber} → track an order
 *
 * Authenticated routes (any logged-in user):
 * → GET /api/orders/my-orders     → get my order history
 *
 * Admin only routes:
 * → GET   /api/orders/admin/all              → all orders
 * → GET   /api/orders/admin/status/{status}  → orders by status
 * → PATCH /api/orders/admin/{id}/status      → update order status
 * → PATCH /api/orders/admin/{id}/payment     → update payment status
 * → PATCH /api/orders/admin/{id}/cancel      → cancel order
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ─────────────────────────────────────────────────────
    // PUBLIC ENDPOINTS
    // ─────────────────────────────────────────────────────

    /**
     * Place a new order — the main checkout endpoint.
     *
     * Works for both guests and logged-in users:
     * → Guest user    → no Authorization header → userEmail = null
     * → Logged-in user → Authorization header present → userEmail = their email
     *
     * @AuthenticationPrincipal → Spring Security injects the
     * currently logged-in user's details here automatically.
     * If no user is logged in → userDetails = null
     *
     * POST http://localhost:8080/api/orders
     */
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        /**
         * Extract email from UserDetails if logged in.
         * null means guest checkout — perfectly valid.
         */
        String userEmail = userDetails != null
                ? userDetails.getUsername()
                : null;

        OrderResponse response = orderService.placeOrder(request, userEmail);
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Track an order by order number.
     * Public — guest customers can track without logging in.
     *
     * GET http://localhost:8080/api/orders/track/ORD-20240101-1234
     */
    @GetMapping("/track/{orderNumber}")
    public ResponseEntity<OrderResponse> trackOrder(
            @PathVariable String orderNumber) {
        return ResponseEntity.ok(
                orderService.getOrderByOrderNumber(orderNumber)
        );
    }

    // ─────────────────────────────────────────────────────
    // AUTHENTICATED USER ENDPOINTS
    // ─────────────────────────────────────────────────────

    /**
     * Get order history for the logged-in user.
     * Requires any valid JWT token (ROLE_USER or ROLE_ADMIN).
     *
     * @AuthenticationPrincipal → injects the logged-in user
     * We use their email to find their orders in DB
     *
     * GET http://localhost:8080/api/orders/my-orders
     */
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                orderService.getMyOrders(userDetails.getUsername())
        );
    }

    // ─────────────────────────────────────────────────────
    // ADMIN ONLY ENDPOINTS
    // ─────────────────────────────────────────────────────

    /**
     * Get all orders.
     * Used in admin dashboard order management table.
     *
     * GET http://localhost:8080/api/orders/admin/all
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    /**
     * Get orders filtered by status.
     * Used in admin dashboard to filter orders.
     *
     * GET http://localhost:8080/api/orders/admin/status/PROCESSING
     * GET http://localhost:8080/api/orders/admin/status/SHIPPED
     * GET http://localhost:8080/api/orders/admin/status/DELIVERED
     * GET http://localhost:8080/api/orders/admin/status/CANCELED
     */
    @GetMapping("/admin/status/{status}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<OrderResponse>> getOrdersByStatus(
            @PathVariable Order.OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    /**
     * Update order status.
     * Used by admin to move order through the flow.
     *
     * PATCH http://localhost:8080/api/orders/admin/1/status?newStatus=SHIPPED
     *
     * @RequestParam → reads the query parameter from URL
     * e.g. ?newStatus=SHIPPED
     */
    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam Order.OrderStatus newStatus) {
        return ResponseEntity.ok(
                orderService.updateOrderStatus(id, newStatus)
        );
    }

    /**
     * Update payment status.
     * Used by admin when COD payment is confirmed.
     *
     * PATCH http://localhost:8080/api/orders/admin/1/payment?newStatus=PAID
     */
    @PatchMapping("/admin/{id}/payment")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<OrderResponse> updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam Order.PaymentStatus newStatus) {
        return ResponseEntity.ok(
                orderService.updatePaymentStatus(id, newStatus)
        );
    }

    /**
     * Cancel an order.
     * Restores stock automatically.
     * Only PROCESSING orders can be canceled.
     *
     * PATCH http://localhost:8080/api/orders/admin/1/cancel
     */
    @PatchMapping("/admin/{id}/cancel")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }
}