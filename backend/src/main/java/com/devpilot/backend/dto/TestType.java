package com.devpilot.backend.dto;

/**
 * Controlled category for test types.
 * Unknown values from the AI are normalized to UNIT rather than crashing.
 */
public enum TestType {
    UNIT,
    EDGE_CASE,
    ERROR_HANDLING,
    VALIDATION,
    BOUNDARY,
    INTEGRATION;

    public static TestType fromString(String value) {
        if (value == null || value.isBlank()) {
            return UNIT;
        }
        try {
            return TestType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNIT;
        }
    }
}
