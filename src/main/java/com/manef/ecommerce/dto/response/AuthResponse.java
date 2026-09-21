package com.manef.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response sent to the customer after successful
 * login or registration.
 *
 * The frontend (Next.js) receives this and:
 * 1. Stores the JWT token in a cookie or localStorage
 * 2. Uses the token in every future request header:
 *    Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
 * 3. Uses role to decide what to show:
 *    ROLE_ADMIN  → show admin dashboard link
 *    ROLE_USER   → show regular customer view
 *
 * Example response sent to frontend:
 * {
 *   "token": "eyJhbGciOiJIUzI1NiJ9...",
 *   "type": "Bearer",
 *   "id": 1,
 *   "fullName": "Ayouni",
 *   "email": "ayouni@gmail.com",
 *   "role": "ROLE_USER"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * The JWT token the frontend must store
     * and send with every protected request
     */
    private String token;

    /**
     * Token type — always "Bearer" for JWT
     * Frontend uses this to build the header:
     * Authorization: Bearer <token>
     */
    @Builder.Default
    private String type = "Bearer";

    /**
     * User's database ID
     */
    private Long id;

    /**
     * User's full name — shown in the UI
     * e.g. "Welcome back, Ayouni!"
     */
    private String fullName;

    /**
     * User's email address
     */
    private String email;

    /**
     * User's role — used by Next.js to
     * protect admin routes on the frontend
     */
    private String role;
}