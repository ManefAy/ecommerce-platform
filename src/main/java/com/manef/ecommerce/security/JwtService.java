package com.manef.ecommerce.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service responsible for everything JWT related:
 * → Generating tokens after login
 * → Extracting data from tokens
 * → Validating tokens on every request
 *
 * @Service → marks this as a Spring-managed bean
 * so we can inject it anywhere with @Autowired
 */
@Service
public class JwtService {

    /**
     * @Value → reads the value from application.properties
     * jwt.secret=404E635266556A58...
     * This is the secret key used to sign and verify tokens
     * NEVER hardcode this — always read from properties
     */
    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * @Value → reads token expiration from application.properties
     * jwt.expiration=86400000 (24 hours in milliseconds)
     */
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // ─────────────────────────────────────────────────────
    // PUBLIC METHODS — used by other classes
    // ─────────────────────────────────────────────────────

    /**
     * Extract the email (subject) from a JWT token.
     * Called by JwtAuthFilter to identify the user.
     *
     * @param token → the JWT token string
     * @return      → the email stored inside the token
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Generate a new JWT token for a user after login.
     * Called by AuthService after successful login.
     *
     * @param userDetails → the logged-in user
     * @return            → the JWT token string
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Validate a token against a user.
     * Called by JwtAuthFilter on every protected request.
     *
     * Checks two things:
     * 1. Does the email in the token match this user?
     * 2. Is the token still valid (not expired)?
     *
     * @param token       → the JWT token from the request header
     * @param userDetails → the user loaded from DB
     * @return            → true if valid, false if not
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return (email.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // ─────────────────────────────────────────────────────
    // PRIVATE METHODS — internal helpers
    // ─────────────────────────────────────────────────────

    /**
     * Generate a token with extra claims (data inside the token).
     * We can store additional data like role inside the token.
     *
     * @param extraClaims → additional data to store in token
     * @param userDetails → the user
     * @return            → the JWT token string
     */
    private String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails) {

        return Jwts.builder()
                // extra data stored in token (e.g. role)
                .setClaims(extraClaims)
                // who this token belongs to (email)
                .setSubject(userDetails.getUsername())
                // when was the token created
                .setIssuedAt(new Date(System.currentTimeMillis()))
                // when does the token expire
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                // sign the token with our secret key
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Check if a token has expired.
     *
     * @param token → the JWT token
     * @return      → true if expired, false if still valid
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extract the expiration date from a token.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic method to extract any claim from a token.
     * A "claim" is a piece of data stored inside the token.
     *
     * @param token          → the JWT token
     * @param claimsResolver → which claim to extract
     * @return               → the extracted claim value
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract ALL claims from a token.
     * This also VERIFIES the token signature —
     * if the token was tampered with, this throws an exception.
     *
     * @param token → the JWT token
     * @return      → all claims stored in the token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Convert the secret string from application.properties
     * into a cryptographic Key object that jjwt can use.
     */
    private Key getSigningKey() {
        byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}