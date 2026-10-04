package com.manef.ecommerce.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Service for uploading and deleting images on Cloudinary.
 *
 * Cloudinary is a cloud-based image management service.
 * We use it to store product images so they:
 * → Never get lost if server restarts
 * → Load fast via CDN
 * → Can be automatically optimized
 */
@Slf4j
@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Initialize Cloudinary with credentials
     * from application.properties
     */
    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {

        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    // ─────────────────────────────────────────────────────
    // UPLOAD IMAGE
    // ─────────────────────────────────────────────────────

    /**
     * Uploads an image file to Cloudinary.
     *
     * @param file      → the image file from the request
     * @param folder    → the folder in Cloudinary to store it
     *                    e.g. "biotouch/products"
     * @return          → the secure URL of the uploaded image
     *                    e.g. "https://res.cloudinary.com/..."
     */
    public String uploadImage(MultipartFile file, String folder) {
        try {
            /**
             * Upload the file to Cloudinary.
             * ObjectUtils.asMap() → options for the upload:
             * → folder: organizes images in Cloudinary dashboard
             * → resource_type: "auto" detects image/video automatically
             */
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "auto"
                    )
            );

            /**
             * Cloudinary returns a map with the upload result.
             * "secure_url" → the HTTPS URL of the uploaded image
             */
            String imageUrl = (String) uploadResult.get("secure_url");
            log.info("Image uploaded successfully: {}", imageUrl);
            return imageUrl;

        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary: {}", e.getMessage());
            throw new RuntimeException("Failed to upload image: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────
    // DELETE IMAGE
    // ─────────────────────────────────────────────────────

    /**
     * Deletes an image from Cloudinary by its public ID.
     *
     * The public ID is the part of the URL after the folder:
     * URL: https://res.cloudinary.com/cloud/image/upload/biotouch/products/abc123
     * Public ID: biotouch/products/abc123
     *
     * @param publicId → the Cloudinary public ID of the image
     */
    public void deleteImage(String publicId) {
        try {
            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.emptyMap()
            );
            log.info("Image deleted from Cloudinary: {}", publicId);
        } catch (IOException e) {
            log.error("Failed to delete image from Cloudinary: {}", e.getMessage());
        }
    }

    /**
     * Extracts the public ID from a Cloudinary URL.
     * Used when we need to delete an image by its URL.
     *
     * Example:
     * URL: https://res.cloudinary.com/cloud/image/upload/v123/biotouch/products/abc
     * Returns: biotouch/products/abc
     */
    public String extractPublicId(String imageUrl) {
        try {
            // Split by "upload/" and take everything after
            String[] parts = imageUrl.split("upload/");
            if (parts.length < 2) return null;

            // Remove the version number (v1234567/) if present
            String afterUpload = parts[1];
            if (afterUpload.startsWith("v")) {
                afterUpload = afterUpload.substring(
                        afterUpload.indexOf("/") + 1
                );
            }

            // Remove file extension (.jpg, .png, etc.)
            int dotIndex = afterUpload.lastIndexOf(".");
            if (dotIndex > 0) {
                afterUpload = afterUpload.substring(0, dotIndex);
            }

            return afterUpload;
        } catch (Exception e) {
            log.error("Failed to extract public ID from URL: {}", imageUrl);
            return null;
        }
    }
}