package com.manef.ecommerce.service;

import com.manef.ecommerce.dto.request.LoginRequest;
import com.manef.ecommerce.dto.request.RegisterRequest;
import com.manef.ecommerce.dto.response.AuthResponse;
import com.manef.ecommerce.entity.User;
import com.manef.ecommerce.repository.UserRepository;
import com.manef.ecommerce.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Handles all authentication logic:
 * → Registration (create new account)
 * → Login (verify credentials + return JWT)
 *
 * @Service → marks this as a Spring-managed service bean
 * @RequiredArgsConstructor → Lombok generates constructor
 * for all final fields (constructor injection)
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // ─────────────────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────────────────

    /**
     * Creates a new customer account.
     *
     * Flow:
     * 1. Check if email is already registered
     * 2. Hash the password with BCrypt
     * 3. Save the user to MySQL
     * 4. Generate a JWT token
     * 5. Return the token + user info
     *
     * @param request → RegisterRequest DTO from the frontend
     * @return        → AuthResponse with JWT token
     */
    public AuthResponse register(RegisterRequest request) {

        // ── Step 1: Check if email already exists ──────
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException(
                "Email already registered: " + request.getEmail()
            );
        }

        // ── Step 2 & 3: Build and save the User entity ─
        /**
         * @Builder pattern we wrote in User.java
         * We hash the password with BCrypt here —
         * NEVER store plain text passwords
         */
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.ROLE_USER)
                .build();

        // Save to MySQL — Hibernate generates the INSERT SQL
        User savedUser = userRepository.save(user);

        // ── Step 4: Generate JWT token ─────────────────
        /**
         * We need a UserDetails object to generate the token.
         * Spring Security's built-in User class implements UserDetails.
         * We build it manually here from our saved user.
         */
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .builder()
                .username(savedUser.getEmail())
                .password(savedUser.getPassword())
                .authorities(savedUser.getRole().name())
                .build();

        String token = jwtService.generateToken(userDetails);

        // ── Step 5: Build and return the response ──────
        return AuthResponse.builder()
                .token(token)
                .id(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .build();
    }

    // ─────────────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────────────

    /**
     * Authenticates an existing user and returns a JWT token.
     *
     * Flow:
     * 1. AuthenticationManager verifies email + password
     *    → If wrong credentials → throws exception automatically
     *    → Spring Security handles the 401 response
     * 2. Load the user from MySQL
     * 3. Generate a JWT token
     * 4. Return the token + user info
     *
     * @param request → LoginRequest DTO from the frontend
     * @return        → AuthResponse with JWT token
     */
    public AuthResponse login(LoginRequest request) {

        // ── Step 1: Verify credentials ─────────────────
        /**
         * UsernamePasswordAuthenticationToken →
         * wraps the email + password into an object
         * Spring Security understands.
         *
         * authenticationManager.authenticate() →
         * internally calls UserDetailsServiceImpl.loadUserByUsername()
         * then compares the BCrypt hashed passwords.
         *
         * If credentials are wrong → BadCredentialsException is thrown
         * automatically → Spring Security returns 401 Unauthorized
         */
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // ── Step 2: Load user from MySQL ───────────────
        /**
         * At this point authentication succeeded.
         * We load the full user to get their details
         * for the response.
         *
         * orElseThrow → should never happen here since
         * authentication already confirmed the user exists
         */
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        // ── Step 3: Generate JWT token ─────────────────
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRole().name())
                .build();

        String token = jwtService.generateToken(userDetails);

        // ── Step 4: Build and return the response ──────
        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}