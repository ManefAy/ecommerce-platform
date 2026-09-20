package com.manef.ecommerce.repository;

import com.manef.ecommerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Find a category by its slug.
     * Used in Next.js routes: /category/electronics
     *
     * Generated SQL:
     * SELECT * FROM categories WHERE slug = ?
     */
    Optional<Category> findBySlug(String slug);

    /**
     * Get all top-level categories (no parent).
     * Used to build the main navigation menu.
     *
     * Generated SQL:
     * SELECT * FROM categories WHERE parent_id IS NULL
     */
    List<Category> findByParentIsNull();

    /**
     * Get all subcategories of a specific parent.
     * e.g. get all subcategories of "Electronics"
     *
     * Generated SQL:
     * SELECT * FROM categories WHERE parent_id = ?
     */
    List<Category> findByParentId(Long parentId);

    /**
     * Check if a slug is already taken.
     * Used when admin creates or updates a category.
     *
     * Generated SQL:
     * SELECT COUNT(*) > 0 FROM categories WHERE slug = ?
     */
    boolean existsBySlug(String slug);
}