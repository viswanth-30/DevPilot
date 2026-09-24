package com.devpilot.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for incoming project creation and update requests (POST / PUT).
 *
 * Using a DTO instead of the entity directly:
 *   - Decouples the API contract from the database schema.
 *   - Prevents accidental over-posting (e.g., a client cannot set createdAt).
 *   - Makes validation constraints clear at the API boundary.
 */
public class ProjectRequestDto {

    @NotBlank(message = "Project name is required")
    @Size(max = 100, message = "Project name must not exceed 100 characters")
    private String name;

    // description is optional — no @NotBlank
    private String description;

    // githubUrl is a plain optional String for now.
    // URL format validation will be added during GitHub integration.
    private String githubUrl;

    // ─── Constructors ─────────────────────────────────────────────────────────

    public ProjectRequestDto() {
    }

    public ProjectRequestDto(String name, String description, String githubUrl) {
        this.name = name;
        this.description = description;
        this.githubUrl = githubUrl;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }
}
