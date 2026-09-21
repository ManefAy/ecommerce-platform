package com.manef.ecommerce.security;

import com.manef.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Tells Spring Security HOW to load a user from the database.
 *
 * Spring Security does not know about our User entity or
 * our UserRepository — it only knows about the
 * UserDetailsService interface.
 *
 * By implementing UserDetailsService we are saying:
 * "When Spring Security needs to find a user,
 *  use THIS method to load them from our MySQL database"
 *
 * @RequiredArgsConstructor → Lombok generates a constructor
 * for all final fields — this is how Spring injects
 * UserRepository into this class (constructor injection)
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    /**
     * final → this field is set once via constructor injection
     * and never changes — this is the recommended way
     * to inject dependencies in Spring Boot
     */
    private final UserRepository userRepository;

    /**
     * Load a user from the database by their email address.
     * Spring Security calls this method automatically
     * during authentication.
     *
     * @param email → the email entered by the user at login
     * @return      → a UserDetails object Spring Security understands
     * @throws UsernameNotFoundException → if no user found with this email
     */
    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        /**
         * Find the user in MySQL by email.
         * If not found → throw exception with clear message.
         * Spring Security catches this and returns 401 Unauthorized.
         */
        var user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found with email: " + email
                        )
                );

        /**
         * Convert our User entity into a Spring Security
         * UserDetails object.
         *
         * Spring Security's built-in User class takes:
         * 1. username  → we use email as the username
         * 2. password  → the BCrypt hashed password from DB
         * 3. authorities → the user's roles/permissions
         *
         * SimpleGrantedAuthority → converts "ROLE_USER" or
         * "ROLE_ADMIN" string into a Spring Security permission
         */
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(user.getRole().name()))
        );
    }
}