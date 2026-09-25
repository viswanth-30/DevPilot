package com.devpilot.backend.dto;

import java.util.List;

/**
 * Response DTO for the AI test suggestions endpoint.
 */
public class AiTestSuggestionResponseDto {

    private Long projectId;
    private String path;
    private String summary;
    private List<AiTestSuggestionDto> tests;

    public AiTestSuggestionResponseDto() {
    }

    public AiTestSuggestionResponseDto(Long projectId, String path, String summary, List<AiTestSuggestionDto> tests) {
        this.projectId = projectId;
        this.path = path;
        this.summary = summary;
        this.tests = tests;
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

    public List<AiTestSuggestionDto> getTests() {
        return tests;
    }

    public void setTests(List<AiTestSuggestionDto> tests) {
        this.tests = tests;
    }
}
