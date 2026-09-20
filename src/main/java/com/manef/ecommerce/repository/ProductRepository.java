package com.manef.ecommerce.repository;

import com.manef.ecommerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find a product by its slug.
     * Used in Next.js product detail page: /products/nike-air-max
     *
     * Generated SQL:
     * SELECT * FROM products WHERE slug = ?
     */
    Optional<Product> findBySlug(String slug);

    /**
     * Get all active products belonging to a specific category.
     * Used in Next.js category page: /category/phones
     *
     * Generated SQL:
     * SELECT * FROM products WHERE category_id = ? AND active = true
     */
    List<Product> findByCategoryIdAndActiveTrue(Long categoryId);

    /**
     * Get all active products only.
     * Used in the storefront homepage and product listing.
     *
     * Generated SQL:
     * SELECT * FROM products WHERE active = true
     */
    List<Product> findByActiveTrue();

    /**
     * Check if a slug is already taken.
     * Used when admin creates or updates a product.
     *
     * Generated SQL:
     * SELECT COUNT(*) > 0 FROM products WHERE slug = ?
     */
    boolean existsBySlug(String slug);

    /**
     * Search products by keyword in title or description.
     * Used in the storefront search bar.
     *
     * @Query → we write the JPQL query manually here because
     * the method name would be too long and complex.
     *
     * JPQL is like SQL but uses entity/field names
     * not table/column names:
     * → "Product" not "products"
     * → "p.title" not "title"
     * → "p.active" not "active"
     *
     * LOWER() and %:keyword% → case-insensitive search
     * e.g. searching "NIKE" will find "Nike Air Max"
     */
    @Query("SELECT p FROM Product p WHERE p.active = true " +
           "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Product> searchByKeyword(@Param("keyword") String keyword);

    /**
     * Get all products belonging to a category including
     * products in its subcategories.
     * e.g. searching "Electronics" also returns
     * products in "Phones" and "Laptops"
     *
     * Generated SQL:
     * SELECT * FROM products WHERE category_id IN
     * (SELECT id FROM categories WHERE parent_id = ?)
     * AND active = true
     */
    @Query("SELECT p FROM Product p WHERE p.active = true " +
           "AND (p.category.id = :categoryId " +
           "OR p.category.parent.id = :categoryId)")
    List<Product> findByCategoryOrSubCategory(@Param("categoryId") Long categoryId);
}