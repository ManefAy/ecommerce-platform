package com.manef.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;


/**
 * Represents a user in the system.
 * Can be a regular customer (ROLE_USER) or an admin (ROLE_ADMIN).
 * Maps to the "users" table in MySQL.
 * We use "users" not "user" because "user" is a reserved keyword in MySQL.
 */

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class User {
	
	/**
     * @Id → this field is the Primary Key
     * @GeneratedValue → MySQL auto-increments it (1, 2, 3...)
     * GenerationType.IDENTITY → lets MySQL handle the ID generation
     */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	 /**
     * @Column(nullable = false) → this column cannot be NULL in the DB
     * length = 100 → VARCHAR(100) in MySQL
     */
	@Column(nullable = false, length = 100)
	private String fullName;
	
	/**
     * unique = true → MySQL creates a UNIQUE INDEX on this column
     * No two users can share the same email
     */
	@Column(nullable = false, unique = true , length = 100)
	private String email;
	
	/**
     * We store a BCrypt-hashed password here, NEVER plain text
     * BCrypt hashes are always 60 characters long
     */
	@Column(nullable = false)
	private String password;
	
	/**
     * @Enumerated(EnumType.STRING) → stores the enum as text in DB
     * Stores "ROLE_USER" or "ROLE_ADMIN", not 0 or 1
     * String is safer — if you reorder the enum, numbers shift, strings don't
     */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false , length = 20)
	private Role role;
	
	/**
     * @CreationTimestamp → Hibernate automatically sets this to
     * the current timestamp when the record is first inserted
     * updatable = false → this value never changes after creation
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * The Role enum is defined INSIDE the User class because
     * it belongs exclusively to User and has no meaning elsewhere
     */
    public enum Role {
        ROLE_USER,
        ROLE_ADMIN
    }
	
	
}
