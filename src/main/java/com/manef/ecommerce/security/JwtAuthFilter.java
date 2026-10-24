package com.manef.ecommerce.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
 * 5. If invalid, expired or malformed → ignores the token.
 *    Public routes still work. Protected routes get a clean
 *    401 from the authenticationEntryPoint in SecurityConfig.
 *
 * Flow:
 * Request → JwtAuthFilter → SecurityConfig → Controller
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
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ─────────────────────────────────────────────────────
        // STEP 2 — Extract the token from the header
        // ─────────────────────────────────────────────────────
        // substring(7) removes "Bearer " (7 characters)
        final String jwt = authHeader.substring(7);

        try {
            // ─────────────────────────────────────────────────
            // STEP 3 — Extract email from token
            // ─────────────────────────────────────────────────
            /**
             * Throws JwtException if the token is expired,
             * tampered with or malformed, and
             * IllegalArgumentException if it is empty.
             */
            final String userEmail = jwtService.extractEmail(jwt);

            if (userEmail != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

                // ─────────────────────────────────────────────
                // STEP 4 — Load user from database
                // ─────────────────────────────────────────────
                // Throws UsernameNotFoundException if the
                // user was deleted after the token was issued
                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(userEmail);

                // ─────────────────────────────────────────────
                // STEP 5 — Validate the token
                // ─────────────────────────────────────────────
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );
                    SecurityContextHolder.getContext()
                            .setAuthentication(authToken);
                }
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            /**
             * Bad token → treat the request as anonymous.
             * We do NOT stop the request here, so a stale token
             * never breaks public pages like product browsing.
             */
            SecurityContextHolder.clearContext();
        }

        // ─────────────────────────────────────────────────────
        // STEP 6 — Continue to the next filter/controller
        // ─────────────────────────────────────────────────────
        filterChain.doFilter(request, response);
    }
}