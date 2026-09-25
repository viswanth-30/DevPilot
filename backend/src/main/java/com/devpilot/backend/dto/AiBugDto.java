package com.devpilot.backend.dto;

/**
 * Represents a single detected bug in a source file.
 */
public class AiBugDto {

    private BugSeverity severity;
    private String title;
    private String description;
    private String lineReference;
    private String suggestion;

    public AiBugDto() {
    }

    public AiBugDto(BugSeverity severity, String title, String description,
                    String lineReference, String suggestion) {
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.lineReference = lineReference;
        this.suggestion = suggestion;
    }

    public BugSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(BugSeverity severity) {
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLineReference() {
        return lineReference;
    }

    public void setLineReference(String lineReference) {
        this.lineReference = lineReference;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
}
