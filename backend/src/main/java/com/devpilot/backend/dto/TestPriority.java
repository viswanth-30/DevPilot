package com.devpilot.backend.dto;

/**
 * Controlled priority for test suggestions.
 * Unknown values from the AI are normalized to LOW rather than crashing.
 */
public enum TestPriority {
    HIGH,
    MEDIUM,
    LOW;

    public static TestPriority fromString(String value) {
        if (value == null || value.isBlank()) {
            return LOW;
        }
        try {
            return TestPriority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return LOW;
        }
    }
}
