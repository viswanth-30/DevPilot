package com.devpilot.backend.dto;

/**
 * Represents a single code improvement suggestion.
 */
public class AiImprovementDto {

    private ImprovementCategory category;
    private ImprovementPriority priority;
    private String title;
    private String description;
    private String lineReference;
    private String recommendation;

    public AiImprovementDto() {
    }

    public AiImprovementDto(ImprovementCategory category, ImprovementPriority priority,
                            String title, String description, String lineReference,
                            String recommendation) {
        this.category = category;
        this.priority = priority;
        this.title = title;
        this.description = description;
        this.lineReference = lineReference;
        this.recommendation = recommendation;
    }

    public ImprovementCategory getCategory() {
        return category;
    }

    public void setCategory(ImprovementCategory category) {
        this.category = category;
    }

    public ImprovementPriority getPriority() {
        return priority;
    }

    public void setPriority(ImprovementPriority priority) {
        this.priority = priority;
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

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
}
