package com.devpilot.backend.dto;

import java.time.LocalDateTime;

/**
 * DTO for outgoing project data in API responses (GET / POST / PUT).
 *
 * Returning a DTO instead of the entity directly:
 *   - Hides JPA-specific fields and annotations from the API consumer.
 *   - Gives us full control over the JSON shape without touching the entity.
 *   - Prevents serialisation issues (e.g., lazy-loaded Hibernate proxies).
 */
public class ProjectResponseDto {

    private Long id;
    private String name;
    private String description;
    private String githubUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ─── Constructors ─────────────────────────────────────────────────────────

    public ProjectResponseDto() {
    }

    public ProjectResponseDto(Long id, String name, String description,
                              String githubUrl, LocalDateTime createdAt,
                              LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.githubUrl = githubUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
