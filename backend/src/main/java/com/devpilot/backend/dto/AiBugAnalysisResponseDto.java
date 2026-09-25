package com.devpilot.backend.dto;

import java.util.List;

/**
 * Response DTO for the AI bug-analysis endpoint.
 */
public class AiBugAnalysisResponseDto {

    private Long projectId;
    private String path;
    private String summary;
    private List<AiBugDto> bugs;

    public AiBugAnalysisResponseDto() {
    }

    public AiBugAnalysisResponseDto(Long projectId, String path, String summary, List<AiBugDto> bugs) {
        this.projectId = projectId;
        this.path = path;
        this.summary = summary;
        this.bugs = bugs;
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

    public List<AiBugDto> getBugs() {
        return bugs;
    }

    public void setBugs(List<AiBugDto> bugs) {
        this.bugs = bugs;
    }
}
