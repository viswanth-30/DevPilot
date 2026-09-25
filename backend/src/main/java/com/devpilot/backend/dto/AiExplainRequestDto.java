package com.devpilot.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class AiExplainRequestDto {

    @NotBlank(message = "Path cannot be blank")
    private String path;

    public AiExplainRequestDto() {
    }

    public AiExplainRequestDto(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
