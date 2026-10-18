package com.manef.ecommerce.service;

import com.manef.ecommerce.dto.request.OrderRequest;
import com.manef.ecommerce.dto.response.DeliveryResponse;
import com.manef.ecommerce.entity.*;
import com.manef.ecommerce.dto.response.OrderResponse;
import com.manef.ecommerce.exception.BusinessException;
import com.manef.ecommerce.exception.ResourceNotFoundException;
import com.manef.ecommerce.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderService — no database needed (repositories are mocked).
 * Run: ./mvnw test -Dtest=OrderServiceTest
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;
    @Mock StockMovementRepository stockMovementRepository;
    @Mock DeliveryApiService deliveryApiService;
    @Mock MetaCapiService metaCapiService;

    @InjectMocks OrderService orderService;

    // ── helpers ──────────────────────────────────────────

    private Product product(long id, int stock) {
        return Product.builder()
                .id(id).title("Shampooing " + id)
                .price(new BigDecimal("25.00"))
                .stockQuantity(stock)
                .weight(new BigDecimal("0.50"))
                .slug("p-" + id).active(true)
                .build();
    }

    private Order order(long id, Order.OrderStatus status) {
        return Order.builder()
                .id(id).orderNumber("ORD-TEST-" + id)
                .customerName("Test").customerPhone("+21612345678")
                .shippingAddress("Rue 1").city("Tunis")
                .totalAmount(new BigDecimal("50.00"))
                .shippingFee(BigDecimal.ZERO)
                .paymentMethod(Order.PaymentMethod.CASH_ON_DELIVERY)
                .paymentStatus(Order.PaymentStatus.UNPAID)
                .orderStatus(status)
                .build();
    }

    private OrderRequest.OrderItemRequest line(long productId, int qty) {
        OrderRequest.OrderItemRequest line = new OrderRequest.OrderItemRequest();
        line.setProductId(productId);
        line.setQuantity(qty);
        return line;
    }

    private OrderRequest orderRequest(OrderRequest.OrderItemRequest... lines) {
        OrderRequest request = new OrderRequest();
        request.setCustomerName("Test");
        request.setCustomerPhone("+21612345678");
        request.setShippingAddress("Rue 1");
        request.setCity("Tunis");
        request.setItems(List.of(lines));
        return request;
    }

    // ── status transitions ───────────────────────────────

    @Test
    void statusCanMoveForwardProcessingToShipped() {
        Order order = order(1, Order.OrderStatus.PROCESSING);
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.updateOrderStatus(1L, Order.OrderStatus.SHIPPED);

        assertThat(order.getOrderStatus()).isEqualTo(Order.OrderStatus.SHIPPED);
    }

    @Test
    void statusCannotGoBackFromDelivered() {
        Order order = order(1, Order.OrderStatus.DELIVERED);
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() ->
                orderService.updateOrderStatus(1L, Order.OrderStatus.PROCESSING))
                .isInstanceOf(BusinessException.class);

        assertThat(order.getOrderStatus()).isEqualTo(Order.OrderStatus.DELIVERED);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void statusCannotSkipFromProcessingToDelivered() {
        Order order = order(1, Order.OrderStatus.PROCESSING);
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() ->
                orderService.updateOrderStatus(1L, Order.OrderStatus.DELIVERED))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void settingCanceledThroughStatusEndpointRestoresStock() {
        Order order = order(1, Order.OrderStatus.PROCESSING);
        Product product = product(10, 4);
        OrderItem item = OrderItem.builder().id(100L).order(order).product(product)
                .quantity(2).unitPrice(new BigDecimal("25.00"))
                .subTotal(new BigDecimal("50.00")).build();
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(item));
        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.updateOrderStatus(1L, Order.OrderStatus.CANCELED);

        assertThat(order.getOrderStatus()).isEqualTo(Order.OrderStatus.CANCELED);
        assertThat(product.getStockQuantity()).isEqualTo(6);
        verify(stockMovementRepository, times(1)).save(any(StockMovement.class));
    }

    @Test
    void cancelingTwiceIsRejectedSoStockIsRestoredOnlyOnce() {
        Order order = order(1, Order.OrderStatus.CANCELED);
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(BusinessException.class);

        verify(productRepository, never()).save(any());
        verify(stockMovementRepository, never()).save(any());
    }

    // ── stock checks at checkout ─────────────────────────

    @Test
    void duplicateLinesAreMergedBeforeTheStockCheck() {
        // stock 5, customer sends the same product twice: 3 + 3 = 6 > 5
        when(productRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(product(10, 5)));

        assertThatThrownBy(() ->
                orderService.placeOrder(orderRequest(line(10, 3), line(10, 3)), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void validOrderDecreasesStockByMergedQuantity() {
        Product product = product(10, 5);
        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryApiService.createShipment(any()))
                .thenReturn(DeliveryResponse.builder().success(false)
                        .errorMessage("placeholder").shippingFee(BigDecimal.ZERO).build());

        orderService.placeOrder(orderRequest(line(10, 2), line(10, 1)), null);

        assertThat(product.getStockQuantity()).isEqualTo(2);
        verify(orderItemRepository, times(1)).save(any(OrderItem.class));
    }

    // ── order tracking ───────────────────────────────────

    @Test
    void trackingWithMatchingPhoneReturnsOrder() {
        when(orderRepository.findByOrderNumber("ORD-TEST-1"))
                .thenReturn(Optional.of(order(1, Order.OrderStatus.PROCESSING)));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());

        // Stored as "+21612345678": spaces and missing country code still match
        OrderResponse response = orderService.getOrderByOrderNumber("ORD-TEST-1", "12 345 678");

        assertThat(response.getOrderNumber()).isEqualTo("ORD-TEST-1");
    }

    @Test
    void trackingWithWrongPhoneLooksLikeUnknownOrder() {
        when(orderRepository.findByOrderNumber("ORD-TEST-1"))
                .thenReturn(Optional.of(order(1, Order.OrderStatus.PROCESSING)));

        assertThatThrownBy(() -> orderService.getOrderByOrderNumber("ORD-TEST-1", "99999999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void trackingWithoutPhoneIsRejected() {
        assertThatThrownBy(() -> orderService.getOrderByOrderNumber("ORD-TEST-1", " "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Phone number is required");

        verify(orderRepository, never()).findByOrderNumber(any());
    }

    @Test
    void newOrderNumberHasARandomCode() {
        when(productRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(product(10, 5)));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryApiService.createShipment(any()))
                .thenReturn(DeliveryResponse.builder().success(false)
                        .errorMessage("placeholder").shippingFee(BigDecimal.ZERO).build());

        OrderResponse response = orderService.placeOrder(orderRequest(line(10, 1)), null);

        assertThat(response.getOrderNumber()).matches("ORD-\\d{8}-[2-9A-HJKMNP-Z]{6}");
    }
}