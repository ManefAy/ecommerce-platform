package com.manef.ecommerce.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Our standard internal delivery request.
 * This is OUR format — not any specific company's format.
 *
 * The DeliveryApiService will translate this into
 * whatever format the delivery company expects.
 *
 * This means if we switch delivery companies,
 * we only change the service — nothing else.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryRequest {

    /**
     * The order number — used as reference
     * in the delivery company's system.
     * e.g. "ORD-20260925-0001"
     */
    private String orderNumber;

    /**
     * Customer's full name — printed on the shipping label
     */
    private String customerName;

    /**
     * Customer's phone number — delivery company
     * calls this number to coordinate delivery
     */
    private String customerPhone;

    /**
     * Full street address
     * e.g. "123 Rue de la Paix, Apt 4B"
     */
    private String shippingAddress;

    /**
     * City of delivery
     * Used by delivery company to route the package
     */
    private String city;

    /**
     * Total weight of the package in KG
     * Used to calculate shipping cost
     */
    private BigDecimal weightKg;

    /**
     * Total value of the order in the local currency
     * Required for COD (Cash on Delivery) orders
     * so the delivery person knows how much to collect
     */
    private BigDecimal codAmount;

    /**
     * Optional notes for the delivery person
     * e.g. "Call before delivery"
     *      "Leave at the door"
     */
    private String notes;
}