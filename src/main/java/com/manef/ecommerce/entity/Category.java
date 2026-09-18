package com.manef.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

/**
 * Represents a product category.
 * Supports subcategories via a self-referencing relationship.
 *
 * Example structure in DB:
 *   id=1, name="Electronics", parent_id=NULL   ← top level
 *   id=2, name="Phones",      parent_id=1      ← subcategory of Electronics
 *   id=3, name="Laptops",     parent_id=1      ← subcategory of Electronics
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Display name of the category
     * e.g. "Electronics", "Clothing"
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * URL-friendly version of the name
     * e.g. "electronics", "mens-clothing"
     * Used in Next.js routes: /category/electronics
     * unique = true → no two categories can share a slug
     */
    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    /**
     * Optional description shown on the category page
     * TEXT type → no length limit unlike VARCHAR
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    // ─────────────────────────────────────────────────────
    // SELF-REFERENCING RELATIONSHIP
    // This is what makes subcategories possible
    // ─────────────────────────────────────────────────────

    /**
     * The parent category of this category.
     *
     * @ManyToOne → Many subcategories can have ONE parent
     *              e.g. Phones, Laptops both have parent = Electronics
     *
     * @JoinColumn → The FK column in THIS table is called "parent_id"
     *               In MySQL this becomes: parent_id BIGINT NULL
     *
     * nullable = true → top-level categories have no parent (NULL)
     *
     * FetchType.LAZY → Hibernate will NOT load the parent automatically
     *                  every time we load a category.
     *                  It loads ONLY when we call .getParent()
     *                  This is important for performance —
     *                  without LAZY every category query would also
     *                  query the parent, which queries its parent, etc.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = true)
    private Category parent;

    /**
     * The list of subcategories belonging to this category.
     *
     * @OneToMany → One parent can have MANY children
     *
     * mappedBy = "parent" → tells Hibernate that the "parent" field
     *                        above OWNS this relationship.
     *                        This side is just for reading children.
     *                        No extra FK column is created here.
     *
     * CascadeType.ALL → if we delete Electronics, delete
     *                    Phones and Laptops too automatically
     *
     * orphanRemoval = true → if we remove a subcategory from this list,
     *                         delete it from the DB as well
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Category> subCategories;
}