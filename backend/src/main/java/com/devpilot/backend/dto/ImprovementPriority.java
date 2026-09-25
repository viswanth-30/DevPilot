package com.devpilot.backend.dto;

/**
 * Controlled priority for code improvement suggestions.
 * Unknown values from the AI are normalized to LOW rather than crashing.
 */
public enum ImprovementPriority {
    HIGH,
    MEDIUM,
    LOW;

    public static ImprovementPriority fromString(String value) {
        if (value == null) {
            return LOW;
        }
        try {
            return ImprovementPriority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return LOW;
        }
    }
}
