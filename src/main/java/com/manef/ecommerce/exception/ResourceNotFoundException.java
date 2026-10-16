package com.manef.ecommerce.exception;

/**
 * Thrown when a requested entity (product, category, order...) does not exist.
 * GlobalExceptionHandler turns it into HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}