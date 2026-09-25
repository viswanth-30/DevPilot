package com.devpilot.backend.dto;

/**
 * Controlled severity levels for detected bugs.
 * Normalizes AI-returned severity strings to known values.
 */
public enum BugSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW;

    /**
     * Safely parse a severity string from the AI response.
     * Falls back to LOW for unknown values rather than crashing.
     */
    public static BugSeverity fromString(String value) {
        if (value == null) {
            return LOW;
        }
        try {
            return BugSeverity.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return LOW;
        }
    }
}
