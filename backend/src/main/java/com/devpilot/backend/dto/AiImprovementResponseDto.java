package com.devpilot.backend.dto;

import java.util.List;

/**
 * Response DTO for the AI code improvement endpoint.
 */
public class AiImprovementResponseDto {

    private Long projectId;
    private String path;
    private String summary;
    private List<AiImprovementDto> suggestions;

    public AiImprovementResponseDto() {
    }

    public AiImprovementResponseDto(Long projectId, String path, String summary, List<AiImprovementDto> suggestions) {
        this.projectId = projectId;
        this.path = path;
        this.summary = summary;
        this.suggestions = suggestions;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<AiImprovementDto> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<AiImprovementDto> suggestions) {
        this.suggestions = suggestions;
    }
}
