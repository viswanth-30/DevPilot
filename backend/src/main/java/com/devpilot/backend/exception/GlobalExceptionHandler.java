package com.devpilot.backend.exception;

import com.devpilot.backend.dto.ErrorResponseDto;
import com.devpilot.backend.exception.GitHubApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.MissingServletRequestParameterException;

/**
 * Centralised exception handler for all REST controllers.
 *
 * @RestControllerAdvice means this class intercepts exceptions thrown by any
 * @RestController and converts them into structured JSON error responses.
 * Controllers themselves remain clean — no try/catch blocks needed.
 *
 * All responses use ErrorResponseDto for a consistent, typed error shape:
 *   {
 *     "timestamp": "2026-09-24T10:19:26",
 *     "status": 404,
 *     "error": "Not Found",
 *     "message": "Project not found with id: 5",
 *     "details": null          ← omitted or null for simple errors
 *   }
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles ResourceNotFoundException (e.g., Project with given ID not found).
     * Returns HTTP 404 with a typed ErrorResponseDto body.
     * 'details' is null — there are no per-field details for a simple not-found error.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Handles GitHubApiException — covers all GitHub API failure scenarios:
     *   - Invalid GitHub URL format            → 400 Bad Request
     *   - Repository not connected yet         → 400 Bad Request
     *   - Repository not found on GitHub       → 404 Not Found
     *   - Invalid or expired GitHub token      → 401 Unauthorized
     *   - GitHub API rate limit exceeded       → 429 Too Many Requests
     *   - GitHub API server error              → 502 Bad Gateway
     *
     * The HTTP status is carried by the exception itself (set at the throw site),
     * so the handler uses it directly rather than hardcoding a status here.
     */
    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<ErrorResponseDto> handleGitHubApiException(GitHubApiException ex) {
        ErrorResponseDto body = new ErrorResponseDto(
                ex.getStatus().value(),
                ex.getStatus().getReasonPhrase(),
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    /**
     * Handles @Valid / @Validated failures (e.g., missing required fields, size violations).
     * Collects all field-level errors into a Map<field, message> and places them
     * in the 'details' field of ErrorResponseDto.
     *
     * Example response:
     *   {
     *     "timestamp": "2026-09-24T10:19:26",
     *     "status": 400,
     *     "error": "Validation Failed",
     *     "message": "Request body contains invalid fields",
     *     "details": { "name": "Project name is required" }
     *   }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                "Request body contains invalid fields",
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handles MissingServletRequestParameterException (e.g. missing ?path=)
     * Returns HTTP 400.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
