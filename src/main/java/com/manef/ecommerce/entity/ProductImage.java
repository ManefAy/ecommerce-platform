package com.manef.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Stores multiple images per product.
 * Each row in this table = one image for one product.
 *
 * Example rows in DB:
 * id=1, product_id=1, imageUrl="uploads/nike-front.jpg",  displayOrder=1
 * id=2, product_id=1, imageUrl="uploads/nike-side.jpg",   displayOrder=2
 * id=3, product_id=1, imageUrl="uploads/nike-detail.jpg", displayOrder=3
 *
 * The image with displayOrder=1 is always the main/thumbnail image
 * shown in product listing cards on the storefront.
 */
@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The path or URL of the image.
     * In Phase 2 we will handle file upload and store
     * the relative path here.
     * e.g. "uploads/products/nike-air-max-front.jpg"
     */
    @Column(nullable = false)
    private String imageUrl;

    /**
     * Controls the order images appear in the gallery.
     * displayOrder = 1 → main thumbnail imagec
     * displayOrder = 2 → second image in gallery
     * displayOrder = 3 → third image in gallery
     * and so on...
     */
    @Column(nullable = false)
    private Integer displayOrder;

    /**
     * Which product this image belongs to.
     *
     * @ManyToOne → Many images belong to ONE product
     *
     * @JoinColumn → FK column in this table is "product_id"
     *               This is the OWNING side of the relationship
     *               meaning this table holds the foreign key
     *
     * FetchType.LAZY → don't load the full product object
     *                  every time we load an image
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}