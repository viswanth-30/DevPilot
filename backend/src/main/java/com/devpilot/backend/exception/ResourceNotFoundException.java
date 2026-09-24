package com.devpilot.backend.exception;

/**
 * Thrown by the service layer when a requested resource (e.g., a Project)
 * is not found in the database.
 *
 * The GlobalExceptionHandler catches this and returns an HTTP 404 response
 * with a structured JSON error body — so controllers never need try/catch blocks.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
