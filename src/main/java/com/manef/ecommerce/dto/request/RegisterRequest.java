package com.manef.ecommerce.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Data sent by the user when creating a new account.
 *
 * Validation rules are enforced BEFORE the request
 * reaches the controller — Spring automatically
 * returns a 400 Bad Request if any rule fails.
 */
@Data
public class RegisterRequest {

    /**
     * Full name of the customer
     * @Size → minimum 2 characters, maximum 100 characters
     */
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    /**
     * Email address — must be unique in DB
     * Uniqueness is checked in the service layer
     * Format is validated here by @Email
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    /**
     * Plain text password sent from the frontend.
     * We will hash it with BCrypt in the service
     * before saving to DB — never store plain text.
     *
     * @Size min=8 → enforce strong passwords
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}