package com.manef.ecommerce.config;

import com.manef.ecommerce.security.JwtAuthFilter;
import com.manef.ecommerce.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

/**
 * Spring Security Configuration.
 *
 * This is the most important config file — it decides:
 * → Which routes are public (no login required)
 * → Which routes are protected (login required)
 * → Which routes require ADMIN role
 * → How passwords are encrypted
 * → How authentication works
 * → CORS settings (allows Next.js to call our API)
 *
 * @Configuration → marks this as a Spring config class
 * @EnableWebSecurity → enables Spring Security
 * @EnableMethodSecurity → allows us to use @PreAuthorize
 *   on individual controller methods for fine-grained
 *   access control
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * The main security filter chain.
     * This method defines all the security rules.
     *
     * @Bean → Spring manages this object and injects
     * it wherever SecurityFilterChain is needed
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            // ─────────────────────────────────────────────
            // CSRF — Cross Site Request Forgery Protection
            // ─────────────────────────────────────────────
            /**
             * We DISABLE csrf because:
             * → We use JWT tokens, not cookies for auth
             * → CSRF protection is only needed for
             *   cookie-based authentication
             * → Our Next.js frontend sends JWT in headers
             */
            .csrf(csrf -> csrf.disable())

            // ─────────────────────────────────────────────
            // CORS — Cross Origin Resource Sharing
            // ─────────────────────────────────────────────
            /**
             * Allow Next.js (localhost:3000) to call
             * our Spring Boot API (localhost:8080)
             * Without this, the browser blocks all
             * requests from a different origin
             */
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ─────────────────────────────────────────────
            // ROUTE PERMISSIONS
            // ─────────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth

                /**
                 * PUBLIC ROUTES — no login required
                 * Anyone can access these
                 */

                // Auth endpoints — login and register
                .requestMatchers("/api/auth/**").permitAll()

                // Product browsing — customers can browse
                // without creating an account
                .requestMatchers("/api/products/**").permitAll()
                .requestMatchers("/api/categories/**").permitAll()

                // Order tracking — guest customers can
                // track their order by order number
                .requestMatchers("/api/orders/track/**").permitAll()

                /**
                 * ADMIN ONLY ROUTES — requires ROLE_ADMIN
                 * If a ROLE_USER tries to access these
                 * Spring Security returns 403 Forbidden
                 */
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")

                /**
                 * ALL OTHER ROUTES — require login
                 * Any route not matched above requires
                 * a valid JWT token
                 */
                .anyRequest().authenticated()
            )

            // ─────────────────────────────────────────────
            // SESSION MANAGEMENT
            // ─────────────────────────────────────────────
            /**
             * STATELESS → Spring Security will NOT create
             * or use HTTP sessions.
             * Each request must carry its own JWT token.
             * This is mandatory for REST APIs.
             */
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ─────────────────────────────────────────────
            // AUTHENTICATION PROVIDER
            // ─────────────────────────────────────────────
            /**
             * Tell Spring Security to use our custom
             * authentication provider that loads users
             * from MySQL and verifies BCrypt passwords
             */
            .authenticationProvider(authenticationProvider())

            // ─────────────────────────────────────────────
            // JWT FILTER
            // ─────────────────────────────────────────────
            /**
             * Add our JWT filter BEFORE Spring Security's
             * default username/password filter.
             * This ensures JWT is checked first on
             * every request.
             */
            .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    // ─────────────────────────────────────────────────────
    // CORS CONFIGURATION
    // ─────────────────────────────────────────────────────

    /**
     * Configure CORS to allow Next.js to call our API.
     *
     * In production replace "http://localhost:3000"
     * with your actual frontend domain:
     * e.g. "https://www.yourstore.com"
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allowed origins — who can call our API
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000"
        ));

        // Allowed HTTP methods
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // Allowed headers — Authorization is important
        // for sending JWT tokens
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept"
        ));

        // Allow credentials (cookies, auth headers)
        configuration.setAllowCredentials(true);

        // Apply this CORS config to all routes
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    // ─────────────────────────────────────────────────────
    // AUTHENTICATION PROVIDER
    // ─────────────────────────────────────────────────────

    /**
     * Tells Spring Security HOW to authenticate users:
     * 1. Load user from DB using UserDetailsServiceImpl
     * 2. Verify password using BCryptPasswordEncoder
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        // Use BCrypt for password verification
        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    // ─────────────────────────────────────────────────────
    // PASSWORD ENCODER
    // ─────────────────────────────────────────────────────

    /**
     * BCrypt password encoder.
     *
     * BCrypt automatically:
     * → Adds a random salt to each password
     * → Hashes it 2^10 (1024) times by default
     * → Makes brute force attacks extremely slow
     *
     * NEVER store plain text passwords.
     * NEVER use MD5 or SHA for passwords.
     * ALWAYS use BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ─────────────────────────────────────────────────────
    // AUTHENTICATION MANAGER
    // ─────────────────────────────────────────────────────

    /**
     * AuthenticationManager is used in AuthService
     * to verify email + password during login.
     * We expose it as a Bean so we can inject it
     * in AuthService later.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}