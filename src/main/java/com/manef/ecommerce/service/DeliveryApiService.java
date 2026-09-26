package com.manef.ecommerce.service;

import com.manef.ecommerce.dto.request.DeliveryRequest;
import com.manef.ecommerce.dto.response.DeliveryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

/**
 * Generic Delivery API Service.
 *
 * This service is the ADAPTER between our system
 * and the external delivery company's API.
 *
 * How it works:
 * 1. Receives our standard DeliveryRequest
 * 2. Translates it to the company's format
 * 3. Sends HTTP POST to the company's API
 * 4. Translates their response to our DeliveryResponse
 * 5. Returns our standard DeliveryResponse
 *
 * To switch delivery companies:
 * → Only change this file
 * → OrderService stays exactly the same
 *
 * @Slf4j → Lombok generates a logger automatically
 * We use it to log API calls and errors
 * instead of System.out.println()
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryApiService {

    private final WebClient.Builder webClientBuilder;

    /**
     * Read delivery API config from application.properties
     */
    @Value("${delivery.api.base-url}")
    private String baseUrl;

    @Value("${delivery.api.key}")
    private String apiKey;

    // ─────────────────────────────────────────────────────
    // MAIN METHOD — Create a shipment
    // ─────────────────────────────────────────────────────

    /**
     * Creates a shipment with the delivery company.
     * Called by OrderService right after saving the order.
     *
     * @param request → our standard delivery request
     * @return        → our standard delivery response
     *                  with tracking number and shipping fee
     */
    public DeliveryResponse createShipment(DeliveryRequest request) {

        log.info("Creating shipment for order: {}", request.getOrderNumber());

        try {
            /**
             * Build the request body in the delivery company's format.
             * This is the part you customize for your specific company.
             *
             * Currently uses a generic format that works with
             * most REST-based delivery APIs.
             *
             * Example for a typical Tunisian delivery company:
             * {
             *   "reference": "ORD-20260925-0001",
             *   "recipient_name": "Ayouni Manef",
             *   "recipient_phone": "12345678",
             *   "address": "123 Rue de la Paix",
             *   "city": "Tunis",
             *   "weight": 1.6,
             *   "cod_amount": 159.98,
             *   "notes": "Please call before delivery"
             * }
             */
            String requestBody = buildRequestBody(request);

            /**
             * Send HTTP POST to the delivery company's API.
             *
             * webClientBuilder.build() → creates a WebClient
             * .post()                  → HTTP POST method
             * .uri(baseUrl + "/shipments") → the endpoint
             * .header("Authorization") → API key for authentication
             * .bodyValue(requestBody)  → the JSON body
             * .retrieve()              → execute the request
             * .bodyToMono(String.class)→ get response as String
             * .block()                 → wait for response (blocking)
             */
            String rawResponse = webClientBuilder.build()
                    .post()
                    .uri(baseUrl + "/shipments")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Delivery API response for order {}: {}",
                    request.getOrderNumber(), rawResponse);

            // Parse the response and return our standard format
            return parseResponse(rawResponse, request.getOrderNumber());

        } catch (Exception e) {
            /**
             * If the delivery API call fails:
             * → Log the error
             * → Return a failed response
             * → The order is still saved in MySQL
             * → Admin can manually create the shipment later
             *
             * We never throw an exception here because
             * the order is already placed — we don't want
             * to show an error to the customer just because
             * the delivery API had a temporary issue.
             */
            log.error("Delivery API call failed for order {}: {}",
                    request.getOrderNumber(), e.getMessage());

            return DeliveryResponse.builder()
                    .success(false)
                    .errorMessage("Delivery API unavailable: " + e.getMessage())
                    .shippingFee(BigDecimal.ZERO)
                    .build();
        }
    }

    // ─────────────────────────────────────────────────────
    // HELPERS — Customize these for your delivery company
    // ─────────────────────────────────────────────────────

    /**
     * Builds the request body in your delivery company's format.
     *
     * THIS IS THE ONLY METHOD YOU NEED TO CHANGE
     * when you switch delivery companies or know
     * your company's exact API format.
     *
     * Current format is generic JSON that works with
     * most REST delivery APIs.
     */
    private String buildRequestBody(DeliveryRequest request) {
        return String.format("""
                {
                    "reference": "%s",
                    "recipient_name": "%s",
                    "recipient_phone": "%s",
                    "address": "%s",
                    "city": "%s",
                    "weight": %s,
                    "cod_amount": %s,
                    "notes": "%s"
                }
                """,
                request.getOrderNumber(),
                request.getCustomerName(),
                request.getCustomerPhone(),
                request.getShippingAddress(),
                request.getCity(),
                request.getWeightKg(),
                request.getCodAmount(),
                request.getNotes() != null ? request.getNotes() : ""
        );
    }

    /**
     * Parses the delivery company's response into
     * our standard DeliveryResponse format.
     *
     * THIS IS THE SECOND METHOD YOU CUSTOMIZE
     * for your specific delivery company.
     *
     * Each company returns different field names:
     * → Aramex returns "WaybillNumber"
     * → DHL returns "trackingNumber"
     * → Local companies return "tracking_id"
     *
     * For now we use a simple JSON parser.
     * In Phase 3 step 2 we will use proper
     * Jackson ObjectMapper for robust parsing.
     */
    private DeliveryResponse parseResponse(
            String rawResponse,
            String orderNumber) {

        try {
            /**
             * Simple parsing — looks for common field names
             * that most delivery APIs use.
             *
             * When you know your exact delivery company,
             * replace this with proper JSON parsing
             * using Jackson ObjectMapper.
             */
            if (rawResponse != null &&
               (rawResponse.contains("tracking") ||
                rawResponse.contains("waybill"))) {

                return DeliveryResponse.builder()
                        .success(true)
                        .trackingNumber(
                            extractField(rawResponse, "tracking_number",
                            "TRK-" + orderNumber)
                        )
                        .shippingFee(new BigDecimal("7.00"))
                        .estimatedDelivery("2-3 business days")
                        .rawResponse(rawResponse)
                        .build();
            }

            // Response didn't contain expected fields
            return DeliveryResponse.builder()
                    .success(false)
                    .errorMessage("Unexpected response format: " + rawResponse)
                    .shippingFee(BigDecimal.ZERO)
                    .rawResponse(rawResponse)
                    .build();

        } catch (Exception e) {
            return DeliveryResponse.builder()
                    .success(false)
                    .errorMessage("Failed to parse response: " + e.getMessage())
                    .shippingFee(BigDecimal.ZERO)
                    .rawResponse(rawResponse)
                    .build();
        }
    }

    /**
     * Simple helper to extract a field value from a JSON string.
     * Returns defaultValue if the field is not found.
     *
     * Example:
     * json = {"tracking_number": "TRK123", "fee": 7.00}
     * extractField(json, "tracking_number", "default") → "TRK123"
     */
    private String extractField(
            String json,
            String fieldName,
            String defaultValue) {

        try {
            String searchKey = "\"" + fieldName + "\"";
            int keyIndex = json.indexOf(searchKey);

            if (keyIndex == -1) return defaultValue;

            int valueStart = json.indexOf("\"", keyIndex +
                    searchKey.length() + 1) + 1;
            int valueEnd = json.indexOf("\"", valueStart);

            return json.substring(valueStart, valueEnd);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}