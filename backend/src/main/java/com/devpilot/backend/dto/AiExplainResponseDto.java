package com.devpilot.backend.dto;

public class AiExplainResponseDto {

    private Long projectId;
    private String path;
    private String explanation;

    public AiExplainResponseDto() {
    }

    public AiExplainResponseDto(Long projectId, String path, String explanation) {
        this.projectId = projectId;
        this.path = path;
        this.explanation = explanation;
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

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
