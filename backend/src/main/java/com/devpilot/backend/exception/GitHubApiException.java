package com.devpilot.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the GitHub REST API returns an error or is unreachable.
 *
 * Carries an HttpStatus so the GlobalExceptionHandler can return the
 * appropriate HTTP status code to the client rather than always using 500.
 *
 * Examples:
 *   - Repository not found on GitHub     → HttpStatus.NOT_FOUND (404)
 *   - Invalid / expired token            → HttpStatus.UNAUTHORIZED (401)
 *   - Rate limit exceeded                → HttpStatus.TOO_MANY_REQUESTS (429)
 *   - GitHub API server error            → HttpStatus.BAD_GATEWAY (502)
 *   - Project not yet connected to GitHub→ HttpStatus.BAD_REQUEST (400)
 *   - Invalid GitHub repository URL      → HttpStatus.BAD_REQUEST (400)
 */
public class GitHubApiException extends RuntimeException {

    private final HttpStatus status;

    /**
     * @param status  The HTTP status to return to the API client.
     * @param message Human-readable explanation of what went wrong.
     */
    public GitHubApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * Returns the HTTP status code that should be sent to the client.
     * Used by GlobalExceptionHandler to set the response status.
     */
    public HttpStatus getStatus() {
        return status;
    }
}
