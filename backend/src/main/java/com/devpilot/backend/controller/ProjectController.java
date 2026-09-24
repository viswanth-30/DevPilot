package com.devpilot.backend.controller;

import com.devpilot.backend.dto.ProjectRequestDto;
import com.devpilot.backend.dto.ProjectResponseDto;
import com.devpilot.backend.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller that exposes the Project CRUD API.
 *
 * @RequestMapping("/api/projects") applies the base path to all methods.
 * @Valid on @RequestBody triggers Bean Validation before the method body runs.
 * Any validation failures are automatically handled by GlobalExceptionHandler.
 *
 * Constructor injection: ProjectService is injected via the constructor,
 * making this class easily testable without a running Spring context.
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * POST /api/projects
     * Creates a new project.
     * Returns HTTP 201 Created with the saved project body.
     */
    @PostMapping
    public ResponseEntity<ProjectResponseDto> createProject(
            @Valid @RequestBody ProjectRequestDto requestDto) {
        ProjectResponseDto created = projectService.createProject(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /api/projects
     * Returns all projects.
     * Returns HTTP 200 OK with a JSON array (empty array if none exist).
     */
    @GetMapping
    public ResponseEntity<List<ProjectResponseDto>> getAllProjects() {
        List<ProjectResponseDto> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    /**
     * GET /api/projects/{id}
     * Returns a single project by ID.
     * Returns HTTP 200 OK, or 404 if not found (handled by GlobalExceptionHandler).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponseDto> getProjectById(@PathVariable Long id) {
        ProjectResponseDto project = projectService.getProjectById(id);
        return ResponseEntity.ok(project);
    }

    /**
     * PUT /api/projects/{id}
     * Updates an existing project's fields.
     * Returns HTTP 200 OK with the updated project, or 404 if not found.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponseDto> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequestDto requestDto) {
        ProjectResponseDto updated = projectService.updateProject(id, requestDto);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/projects/{id}
     * Deletes a project by ID.
     * Returns HTTP 204 No Content on success, or 404 if not found.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
