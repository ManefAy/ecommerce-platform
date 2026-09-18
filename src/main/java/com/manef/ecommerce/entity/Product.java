package com.manef.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a product in the store.
 *
 * Key decisions explained:
 *
 * 1. Price uses BigDecimal NOT double/float
 *    → Float/double have rounding errors (19.99 becomes 19.989999...)
 *    → BigDecimal is exact — mandatory for any money value
 *
 * 2. weight is stored in kilograms (kg)
 *    → Required by most Delivery APIs to calculate
 *      shipping cost and generate labels
 *
 * 3. slug is used for SEO-friendly URLs
 *    → /products/nike-air-max instead of /products/1
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The product title shown to customers
     * e.g. "Nike Air Max 2024 — White/Black"
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * Full product description
     * TEXT → no length limit, supports long descriptions
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * The regular selling price
     * precision = 10 → total 10 digits allowed
     * scale = 2      → exactly 2 decimal places
     * Supports values up to: 99999999.99
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Optional sale/discount price
     * nullable → not all products are on sale
     * When this is NOT null, the frontend shows:
     *   Original price: crossed out
     *   Discount price: highlighted in red
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal discountPrice;

    /**
     * How many units are available in stock
     * We decrease this when an order is placed
     * We increase this when stock is restocked or order is canceled
     */
    @Column(nullable = false)
    private Integer stockQuantity;

    /**
     * Product weight in kilograms
     * Required by the Delivery API to:
     *   → Calculate shipping cost
     *   → Generate the shipping label
     * precision=5, scale=2 → supports up to 999.99 kg
     */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    /**
     * SEO-friendly URL identifier
     * e.g. "nike-air-max-2024-white-black"
     * Used in Next.js: /products/nike-air-max-2024-white-black
     * unique = true → no two products share the same slug
     */
    @Column(nullable = false, unique = true, length = 200)
    private String slug;

    /**
     * Controls product visibility to customers
     * true  → product is visible in the storefront
     * false → product is hidden (admin deactivated it)
     *
     * Admins can deactivate products without deleting them
     * This is called "soft visibility" — data stays in DB
     *
     * @Builder.Default → tells @Builder to use true as
     * the default value when not explicitly set
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    // ─────────────────────────────────────────────────────
    // RELATIONSHIPS
    // ─────────────────────────────────────────────────────

    /**
     * Which category this product belongs to
     *
     * @ManyToOne → Many products can belong to ONE category
     *              e.g. iPhone 15, Samsung S24 both belong to "Phones"
     *
     * FetchType.LAZY → don't load category data unless we need it
     *
     * @JoinColumn → FK column in products table is "category_id"
     * nullable = false → every product MUST have a category
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * All images belonging to this product
     *
     * @OneToMany → One product can have MANY images
     * mappedBy = "product" → ProductImage owns the relationship
     * CascadeType.ALL → saving/deleting product also affects its images
     * orphanRemoval → if image is removed from list, delete from DB
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images;

    // ─────────────────────────────────────────────────────
    // AUDIT TIMESTAMPS
    // ─────────────────────────────────────────────────────

    /**
     * Automatically set by Hibernate when product is first created
     * updatable = false → never changes after creation
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Automatically updated by Hibernate every time
     * this product record is modified
     * Useful for admin dashboard — "last updated 2 hours ago"
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}