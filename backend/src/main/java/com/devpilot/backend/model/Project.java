package com.devpilot.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * JPA entity that maps to the 'projects' table in PostgreSQL.
 * Hibernate will auto-create this table on first run (ddl-auto=update).
 */
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Raised from default VARCHAR(255) — GitHub URLs with deep paths can exceed 255 characters.
    @Column(name = "github_url", length = 500)
    private String githubUrl;

    // Set when a GitHub repository is successfully connected to this project.
    // Parsed from githubUrl so that every API call does not re-parse the URL.
    @Column(name = "repo_owner", length = 100)
    private String repoOwner;

    @Column(name = "repo_name", length = 100)
    private String repoName;

    // Fetched from GitHub API on connect — required for file tree calls.
    @Column(name = "default_branch", length = 100)
    private String defaultBranch;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ─── Lifecycle hooks ─────────────────────────────────────────────────────

    /**
     * Called by JPA before a new entity is persisted (INSERT).
     * Sets both createdAt and updatedAt to the current time.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Called by JPA before an existing entity is updated (UPDATE).
     * Only refreshes updatedAt — createdAt is marked updatable = false.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ─── Constructors ─────────────────────────────────────────────────────────

    public Project() {
    }

    public Project(String name, String description, String githubUrl) {
        this.name = name;
        this.description = description;
        this.githubUrl = githubUrl;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public Long getId() {
        return id;
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

    public String getRepoOwner() {
        return repoOwner;
    }

    public void setRepoOwner(String repoOwner) {
        this.repoOwner = repoOwner;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
