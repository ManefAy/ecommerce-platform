package com.manef.ecommerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data sent by the user when logging in.
 *
 * @Data → Lombok shortcut that generates:
 *   @Getter + @Setter + @ToString
 *   + @EqualsAndHashCode + @RequiredArgsConstructor
 *   Perfect for simple DTO classes.
 *
 * Validation annotations:
 * @NotBlank → field cannot be null or empty string
 * @Email    → must be a valid email format
 * These are checked automatically by Spring before
 * the request reaches our controller method.
 */
@Data
public class LoginRequest {

    /**
     * Customer's email address
     * @Email ensures format is valid: "x@x.x"
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    /**
     * Customer's password (plain text here —
     * we compare it against the BCrypt hash in DB)
     */
    @NotBlank(message = "Password is required")
    private String password;
}