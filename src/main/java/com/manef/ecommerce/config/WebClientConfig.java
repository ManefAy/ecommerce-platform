package com.manef.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration class for WebClient.
 *
 * WebClient is Spring Boot's modern HTTP client.
 * We use it to call external REST APIs:
 * → Delivery API (to create shipments and get tracking numbers)
 * → Meta Conversions API (to send purchase events for ad tracking)
 *
 * @Configuration → marks this as a Spring config class
 * @Bean → Spring manages this object and injects it
 * wherever WebClient.Builder is needed
 */
@Configuration
public class WebClientConfig {

    /**
     * Creates a WebClient.Builder bean.
     *
     * We use Builder instead of a pre-built WebClient
     * because each service needs its own base URL
     * and headers configured differently.
     *
     * DeliveryApiService  → uses delivery API base URL
     * MetaCapiService     → uses Meta API base URL
     *
     * Both services will inject this builder and
     * configure it for their specific needs.
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}