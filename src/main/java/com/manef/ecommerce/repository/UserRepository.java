package com.manef.ecommerce.repository;

import com.manef.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository for User entity.
 *
 * JpaRepository<User, Long> means:
 * → User = the entity this repository manages
 * → Long = the type of the primary key (id)
 *
 * @Repository → marks this as a Spring-managed bean
 * Spring will automatically implement this interface at runtime
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their email address.
     * Used during login to look up the user.
     *
     * Spring Data JPA reads the method name and automatically
     * generates this SQL:
     * SELECT * FROM users WHERE email = ?
     *
     * Optional<User> → the user may or may not exist
     * we handle both cases without a NullPointerException
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if an email is already registered.
     * Used during registration to prevent duplicates.
     *
     * Generated SQL:
     * SELECT COUNT(*) > 0 FROM users WHERE email = ?
     */
    boolean existsByEmail(String email);
}