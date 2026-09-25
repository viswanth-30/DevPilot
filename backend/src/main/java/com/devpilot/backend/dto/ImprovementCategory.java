package com.devpilot.backend.dto;

/**
 * Controlled category for code improvement suggestions.
 * Unknown values from the AI are normalized to MAINTAINABILITY rather than crashing.
 */
public enum ImprovementCategory {
    READABILITY,
    MAINTAINABILITY,
    PERFORMANCE,
    ERROR_HANDLING,
    SECURITY,
    DESIGN,
    DUPLICATION,
    NAMING,
    TESTABILITY;

    public static ImprovementCategory fromString(String value) {
        if (value == null) {
            return MAINTAINABILITY;
        }
        try {
            return ImprovementCategory.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MAINTAINABILITY;
        }
    }
}
