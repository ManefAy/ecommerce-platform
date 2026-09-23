package com.manef.ecommerce.controller;

import com.manef.ecommerce.dto.request.LoginRequest;
import com.manef.ecommerce.dto.request.RegisterRequest;
import com.manef.ecommerce.dto.response.AuthResponse;
import com.manef.ecommerce.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for authentication endpoints.
 *
 * @RestController → marks this as a REST controller
 * combines @Controller + @ResponseBody
 * every method returns JSON automatically
 *
 * @RequestMapping → all routes in this controller
 * start with /api/auth
 *
 * @RequiredArgsConstructor → Lombok injects AuthService
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ─────────────────────────────────────────────────────
    // POST /api/auth/register
    // ─────────────────────────────────────────────────────

    /**
     * Register a new customer account.
     *
     * @Valid → triggers validation annotations we wrote
     * in RegisterRequest (@NotBlank, @Email, @Size)
     * If validation fails → Spring returns 400 Bad Request
     * automatically with error details
     *
     * @RequestBody → Spring reads the JSON body from
     * the HTTP request and converts it to RegisterRequest
     *
     * ResponseEntity → lets us control the HTTP status code
     * 201 Created → standard response for successful creation
     *
     * Example request:
     * POST http://localhost:8080/api/auth/register
     * {
     *   "fullName": "Ayouni Manef",
     *   "email": "ayouni@gmail.com",
     *   "password": "mypassword123"
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);

        /**
         * ResponseEntity.status(201).body(response)
         * → HTTP 201 Created + JSON body
         */
        return ResponseEntity.status(201).body(response);
    }

    // ─────────────────────────────────────────────────────
    // POST /api/auth/login
    // ─────────────────────────────────────────────────────

    /**
     * Login with existing credentials.
     * Returns a JWT token on success.
     *
     * Example request:
     * POST http://localhost:8080/api/auth/login
     * {
     *   "email": "ayouni@gmail.com",
     *   "password": "mypassword123"
     * }
     *
     * Example response:
     * {
     *   "token": "eyJhbGciOiJIUzI1NiJ9...",
     *   "type": "Bearer",
     *   "id": 1,
     *   "fullName": "Ayouni Manef",
     *   "email": "ayouni@gmail.com",
     *   "role": "ROLE_USER"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);

        /**
         * ResponseEntity.ok() → HTTP 200 OK + JSON body
         */
        return ResponseEntity.ok(response);
    }
}