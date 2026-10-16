package com.manef.ecommerce.exception;

import com.manef.ecommerce.controller.ProductController;
import com.manef.ecommerce.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks that exceptions become the right HTTP status + JSON "message".
 * Uses a standalone MockMvc: no database, no Spring Security.
 * Run: ./mvnw test -Dtest=GlobalExceptionHandlerTest
 */
class GlobalExceptionHandlerTest {

    private ProductService productService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProductController(productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void notFoundBecomes404WithMessage() throws Exception {
        when(productService.getProductBySlug("missing"))
                .thenThrow(new ResourceNotFoundException("Product not found with slug: missing"));

        mockMvc.perform(get("/api/products/slug/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found with slug: missing"));
    }

    @Test
    void businessRuleBecomes409WithMessage() throws Exception {
        when(productService.getProductById(1L))
                .thenThrow(new BusinessException("Slug already exists: abc"));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Slug already exists: abc"));
    }

    @Test
    void invalidBodyBecomes400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    void unexpectedErrorBecomes500WithoutInternalDetails() throws Exception {
        when(productService.getProductById(2L))
                .thenThrow(new IllegalStateException("db password is wrong"));

        mockMvc.perform(get("/api/products/2"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Something went wrong. Please try again."));
    }
}