package com.manef.ecommerce.service;

import com.manef.ecommerce.entity.Order;
import com.manef.ecommerce.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Meta Conversions API (CAPI) Service.
 *
 * What is Meta CAPI?
 * When a customer buys something, we want to tell
 * Facebook/Instagram about it so our ads can:
 * → Track which ads led to purchases
 * → Optimize ad delivery to find more buyers
 * → Calculate real Return on Ad Spend (ROAS)
 *
 * Why server-side (CAPI) instead of just browser Pixel?
 * → Browser ad blockers block the frontend Pixel
 * → iOS privacy changes hide frontend events
 * → CAPI sends directly from our server → 100% accurate
 * → Facebook recommends using BOTH for best results
 *
 * How it works:
 * 1. Customer places order
 * 2. We save the order to MySQL
 * 3. We ASYNCHRONOUSLY send a "Purchase" event to Meta
 *    → Async means it runs in the background
 *    → Customer gets their confirmation immediately
 *    → Meta receives the event a moment later
 *    → If Meta API is slow → customer never notices
 *
 * @Slf4j → Lombok generates logger automatically
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetaCapiService {

    private final WebClient.Builder webClientBuilder;

    /**
     * Read Meta CAPI credentials from application.properties
     */
    @Value("${meta.capi.access-token}")
    private String accessToken;

    @Value("${meta.capi.pixel-id}")
    private String pixelId;

    /**
     * Meta CAPI endpoint — always this URL
     * We append our pixelId to it dynamically
     */
    private static final String META_CAPI_URL =
            "https://graph.facebook.com/v18.0/";

    // ─────────────────────────────────────────────────────
    // SEND PURCHASE EVENT
    // ─────────────────────────────────────────────────────

    /**
     * Sends a "Purchase" event to Meta Conversions API.
     *
     * @Async → This method runs in a SEPARATE THREAD
     * in the background — the customer doesn't wait for it.
     *
     * Why async?
     * → Meta API can sometimes be slow (1-3 seconds)
     * → We never want to delay the customer's
     *   order confirmation because of an ad tracking call
     * → If Meta is down → order still completes normally
     *
     * @param order      → the completed order
     * @param orderItems → the items in the order
     */
    @Async
    public void sendPurchaseEvent(Order order, List<OrderItem> orderItems) {

        log.info("Sending Meta CAPI Purchase event for order: {}",
                order.getOrderNumber());

        try {
            /**
             * Check if Meta CAPI is configured.
             * If credentials are placeholders → simulate the call.
             * This prevents errors during development
             * when you don't have real Meta credentials yet.
             */
            if (isSimulationMode()) {
                simulatePurchaseEvent(order);
                return;
            }

            // Build the event payload
            String eventPayload = buildPurchaseEventPayload(order, orderItems);

            /**
             * Send to Meta CAPI endpoint:
             * POST https://graph.facebook.com/v18.0/{pixel-id}/events
             *      ?access_token={your-token}
             */
            String response = webClientBuilder.build()
                    .post()
                    .uri(META_CAPI_URL + pixelId + "/events"
                            + "?access_token=" + accessToken)
                    .header("Content-Type", "application/json")
                    .bodyValue(eventPayload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Meta CAPI response for order {}: {}",
                    order.getOrderNumber(), response);

        } catch (Exception e) {
            /**
             * Never throw an exception here.
             * If Meta CAPI fails → just log the error.
             * The order is already saved — customer is happy.
             * We can resend the event manually if needed.
             */
            log.error("Meta CAPI failed for order {}: {}",
                    order.getOrderNumber(), e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────

    /**
     * Builds the Meta CAPI Purchase event payload.
     *
     * Meta expects this exact format:
     * {
     *   "data": [{
     *     "event_name": "Purchase",
     *     "event_time": 1234567890,
     *     "action_source": "website",
     *     "user_data": {
     *       "ph": ["hashed_phone"],
     *       "em": ["hashed_email"]
     *     },
     *     "custom_data": {
     *       "currency": "TND",
     *       "value": 159.98,
     *       "order_id": "ORD-20260925-0001",
     *       "contents": [...]
     *     }
     *   }]
     * }
     *
     * Note: Meta requires phone and email to be
     * SHA-256 hashed for privacy.
     * We will add hashing in production.
     * For now we send them as-is for testing.
     */
    private String buildPurchaseEventPayload(
            Order order,
            List<OrderItem> orderItems) {

        // Current timestamp in seconds (Meta requires seconds not ms)
        long eventTime = System.currentTimeMillis() / 1000;

        // Build contents array from order items
        StringBuilder contents = new StringBuilder("[");
        for (int i = 0; i < orderItems.size(); i++) {
            OrderItem item = orderItems.get(i);
            contents.append(String.format("""
                    {
                        "id": "%s",
                        "quantity": %d,
                        "item_price": %s
                    }
                    """,
                    item.getProduct().getId(),
                    item.getQuantity(),
                    item.getUnitPrice()
            ));
            if (i < orderItems.size() - 1) {
                contents.append(",");
            }
        }
        contents.append("]");

        return String.format("""
                {
                    "data": [{
                        "event_name": "Purchase",
                        "event_time": %d,
                        "action_source": "website",
                        "user_data": {
                            "ph": ["%s"],
                            "client_ip_address": "0.0.0.0",
                            "client_user_agent": "server-side"
                        },
                        "custom_data": {
                            "currency": "TND",
                            "value": %s,
                            "order_id": "%s",
                            "num_items": %d,
                            "contents": %s
                        }
                    }]
                }
                """,
                eventTime,
                order.getCustomerPhone(),
                order.getTotalAmount(),
                order.getOrderNumber(),
                orderItems.size(),
                contents.toString()
        );
    }

    /**
     * Checks if we are in simulation mode.
     * Simulation mode is active when the credentials
     * in application.properties are still placeholders.
     */
    private boolean isSimulationMode() {
        return accessToken.equals("your_meta_access_token") ||
               pixelId.equals("your_pixel_id") ||
               accessToken.isEmpty() ||
               pixelId.isEmpty();
    }

    /**
     * Simulates a Meta CAPI call during development.
     * Just logs what would have been sent to Meta.
     * No actual HTTP call is made.
     */
    private void simulatePurchaseEvent(Order order) {
        log.info("=== META CAPI SIMULATION MODE ===");
        log.info("Would send Purchase event to Meta for:");
        log.info("  Order Number : {}", order.getOrderNumber());
        log.info("  Customer     : {}", order.getCustomerName());
        log.info("  Total Amount : {} TND", order.getTotalAmount());
        log.info("  City         : {}", order.getCity());
        log.info("=== Add real Meta credentials to enable ===");
    }
}