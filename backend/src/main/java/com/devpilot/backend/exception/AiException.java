package com.devpilot.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an AI integration error occurs.
 *
 * Carries an HttpStatus so the GlobalExceptionHandler can return the
 * appropriate HTTP status code to the client.
 */
public class AiException extends RuntimeException {

    private final HttpStatus status;

    public AiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
