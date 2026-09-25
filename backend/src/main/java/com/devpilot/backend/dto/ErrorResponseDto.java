package com.devpilot.backend.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Typed response body for all API error responses.
 *
 * Replaces the raw HashMap<String, Object> previously used in GlobalExceptionHandler.
 * Benefits:
 *   - Type safety: field names are compile-checked, not runtime string literals.
 *   - Consistent JSON shape across every error type.
 *   - Easy to extend (e.g., add an errorCode field later for the frontend).
 *   - Jackson serialises this the same way it would a HashMap — no extra config needed.
 *
 * The 'details' field carries extra data (e.g., per-field validation errors).
 * It is null for simple errors like 404, so Jackson will omit it from the JSON.
 */
public class ErrorResponseDto {

    private final String timestamp;
    private final int status;
    private final String error;
    private final String message;

    /**
     * Optional extra detail payload — used for validation errors to carry
     * the per-field error map. Null for simple errors (404 etc.).
     * Jackson omits null fields by default only if configured; kept here
     * as Object so the handler can pass any structure without casting.
     */
    private final Object details;

    // ─── Constructor ──────────────────────────────────────────────────────────

    /**
     * Full constructor used by GlobalExceptionHandler.
     *
     * @param status  HTTP status code (e.g., 404, 400)
     * @param error   Short error label (e.g., "Not Found", "Validation Failed")
     * @param message Human-readable explanation of the error
     * @param details Optional extra payload (null for simple errors)
     */
    public ErrorResponseDto(int status, String error, String message, Object details) {
        // Produce a consistent, readable ISO-8601 timestamp without nanoseconds.
        this.timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        this.status = status;
        this.error = error;
        this.message = message;
        this.details = details;
    }

    // ─── Getters (no setters — this object is read-only after construction) ───

    public String getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public Object getDetails() {
        return details;
    }
}
