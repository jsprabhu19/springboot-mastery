package com.quickeats.userservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Custom runtime exception representing user service business logic failures.
 */
public class UserException extends RuntimeException {

    private final HttpStatus status;

    public UserException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
