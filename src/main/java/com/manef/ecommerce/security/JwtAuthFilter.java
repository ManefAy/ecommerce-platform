package com.manef.ecommerce.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/**
 * JWT Authentication Filter.
 *
 * This filter runs ONCE on every incoming HTTP request
 * BEFORE it reaches any controller.
 *
 * What it does:
 * 1. Reads the Authorization header from the request
 * 2. Extracts the JWT token
 * 3. Validates the token
 * 4. If valid → tells Spring Security who the user is
 * 5. If invalid → does nothing (Spring Security will
 *    block the request if the route is protected)
 *
 * Flow:
 * Request → JwtAuthFilter → SecurityConfig → Controller
 *
 * extends OncePerRequestFilter → guarantees this filter
 * runs exactly once per request, not multiple times
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // ─────────────────────────────────────────────────────
        // STEP 1 — Read the Authorization header
        // ─────────────────────────────────────────────────────

        /**
         * Every protected request must have this header:
         * Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
         *
         * If the header is missing or does not start with
         * "Bearer " → skip JWT validation and move on.
         * Spring Security will block the request later
         * if the route requires authentication.
         */
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ─────────────────────────────────────────────────────
        // STEP 2 — Extract the token from the header
        // ─────────────────────────────────────────────────────

        /**
         * authHeader = "Bearer eyJhbGciOiJIUzI1NiJ9..."
         * substring(7) removes "Bearer " (7 characters)
         * leaving just the token:
         * "eyJhbGciOiJIUzI1NiJ9..."
         */
        final String jwt = authHeader.substring(7);

        // ─────────────────────────────────────────────────────
        // STEP 3 — Extract email from token
        // ─────────────────────────────────────────────────────

        final String userEmail = jwtService.extractEmail(jwt);

        /**
         * SecurityContextHolder.getContext().getAuthentication()
         * → checks if Spring Security already knows who this user is
         * If null → we need to authenticate them
         * If not null → already authenticated, skip
         */
        if (userEmail != null &&
            SecurityContextHolder.getContext().getAuthentication() == null) {

            // ─────────────────────────────────────────────────────
            // STEP 4 — Load user from database
            // ─────────────────────────────────────────────────────

            /**
             * Load the full user details from MySQL
             * using the email we extracted from the token
             */
            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(userEmail);

            // ─────────────────────────────────────────────────────
            // STEP 5 — Validate the token
            // ─────────────────────────────────────────────────────

            /**
             * Check:
             * 1. Does email in token match the user in DB?
             * 2. Is the token not expired?
             */
            if (jwtService.isTokenValid(jwt, userDetails)) {

                /**
                 * Create an authentication object that
                 * Spring Security understands.
                 * This tells Spring Security:
                 * "This user is authenticated and has
                 *  these roles/permissions"
                 */
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                /**
                 * Add request details (IP address, session ID)
                 * to the authentication object
                 */
                authToken.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                /**
                 * Store the authentication in Spring Security's
                 * context — this is how Spring Security knows
                 * who the current user is for this request
                 */
                SecurityContextHolder.getContext()
                        .setAuthentication(authToken);
            }
        }

        // ─────────────────────────────────────────────────────
        // STEP 6 — Continue to the next filter/controller
        // ─────────────────────────────────────────────────────

        /**
         * Always call this at the end — it passes the request
         * to the next filter in the chain, or to the
         * controller if there are no more filters
         */
        filterChain.doFilter(request, response);
    }
}