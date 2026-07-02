package com.quickeats.restaurantservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom runtime exception representing Restaurant Service business logic failures.
 */
public class RestaurantException extends RuntimeException {

    private final HttpStatus status;

    public RestaurantException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
