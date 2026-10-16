package com.manef.ecommerce.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request is valid but breaks a business rule
 * (insufficient stock, invalid order status change, slug already used...).
 * Default status is 409 Conflict; use the second constructor for 400.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message) {
        this(HttpStatus.CONFLICT, message);
    }

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}