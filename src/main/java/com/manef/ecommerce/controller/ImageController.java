package com.manef.ecommerce.controller;

import com.manef.ecommerce.entity.Product;
import com.manef.ecommerce.entity.ProductImage;
import com.manef.ecommerce.repository.ProductImageRepository;
import com.manef.ecommerce.repository.ProductRepository;
import com.manef.ecommerce.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for product image upload and management.
 *
 * Endpoints:
 * → POST /api/images/products/{productId} → upload image
 * → DELETE /api/images/{imageId}          → delete image
 * → PUT /api/images/{imageId}/order       → update display order
 *
 * All endpoints require ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final CloudinaryService cloudinaryService;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    // ─────────────────────────────────────────────────────
    // UPLOAD IMAGE
    // ─────────────────────────────────────────────────────

    /**
     * Uploads an image for a specific product.
     *
     * @param productId    → which product to add image to
     * @param file         → the image file from the request
     * @param displayOrder → position in the gallery (1 = main image)
     * @return             → the created ProductImage data
     */
    @PostMapping("/products/{productId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> uploadProductImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "displayOrder", defaultValue = "1")
            Integer displayOrder) {

        // Verify product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found: " + productId)
                );

        // Validate file
        if (file.isEmpty()) {
            throw new RuntimeException("Please select a file to upload");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed");
        }

        // Upload to Cloudinary
        String imageUrl = cloudinaryService.uploadImage(
                file,
                "biotouch/products"
        );

        // Save to database
        ProductImage productImage = ProductImage.builder()
                .product(product)
                .imageUrl(imageUrl)
                .displayOrder(displayOrder)
                .build();

        ProductImage saved = productImageRepository.save(productImage);

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("id", saved.getId());
        response.put("imageUrl", saved.getImageUrl());
        response.put("displayOrder", saved.getDisplayOrder());
        response.put("productId", productId);

        return ResponseEntity.status(201).body(response);
    }

    // ─────────────────────────────────────────────────────
    // DELETE IMAGE
    // ─────────────────────────────────────────────────────

    /**
     * Deletes a product image from both
     * Cloudinary and the database.
     *
     * @param imageId → the ID of the ProductImage to delete
     */
    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteProductImage(
            @PathVariable Long imageId) {

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() ->
                        new RuntimeException("Image not found: " + imageId)
                );

        // Delete from Cloudinary
        String publicId = cloudinaryService.extractPublicId(image.getImageUrl());
        if (publicId != null) {
            cloudinaryService.deleteImage(publicId);
        }

        // Delete from database
        productImageRepository.delete(image);

        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────
    // GET IMAGES FOR PRODUCT
    // ─────────────────────────────────────────────────────

    /**
     * Get all images for a specific product.
     * Used by admin dashboard to manage product images.
     */
    @GetMapping("/products/{productId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<ProductImage>> getProductImages(
            @PathVariable Long productId) {

        List<ProductImage> images = productImageRepository
                .findByProductIdOrderByDisplayOrderAsc(productId);

        return ResponseEntity.ok(images);
    }
}