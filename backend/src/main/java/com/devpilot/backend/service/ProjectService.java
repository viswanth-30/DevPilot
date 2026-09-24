package com.devpilot.backend.service;

import com.devpilot.backend.dto.ProjectRequestDto;
import com.devpilot.backend.dto.ProjectResponseDto;
import com.devpilot.backend.exception.ResourceNotFoundException;
import com.devpilot.backend.model.Project;
import com.devpilot.backend.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for Project operations.
 *
 * Responsibilities:
 *   - Orchestrate business logic between Controller and Repository.
 *   - Map between DTOs (API layer) and Entities (persistence layer).
 *   - Throw domain-specific exceptions (ResourceNotFoundException).
 *
 * Constructor injection is used so that dependencies are explicit,
 * immutable, and easily testable (no Spring context needed in unit tests).
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    // ─── Existing method — kept unchanged for HelloController ─────────────────

    public String getProjectMessage() {
        return "DevPilot project service is working";
    }

    // ─── CRUD Operations ──────────────────────────────────────────────────────

    /**
     * Creates a new project from the given request DTO.
     * @param requestDto validated input from the controller
     * @return the saved project as a response DTO
     */
    public ProjectResponseDto createProject(ProjectRequestDto requestDto) {
        Project project = new Project(
                requestDto.getName(),
                requestDto.getDescription(),
                requestDto.getGithubUrl()
        );
        Project saved = projectRepository.save(project);
        return toResponseDto(saved);
    }

    /**
     * Retrieves all projects.
     * @return list of all projects as response DTOs
     */
    public List<ProjectResponseDto> getAllProjects() {
        return projectRepository.findAll()
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single project by its ID.
     * @param id the project primary key
     * @return the project as a response DTO
     * @throws ResourceNotFoundException if no project exists with the given ID
     */
    public ProjectResponseDto getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id: " + id));
        return toResponseDto(project);
    }

    /**
     * Updates an existing project with new values from the request DTO.
     * Only name, description, and githubUrl are updatable.
     * Timestamps are managed automatically by @PrePersist / @PreUpdate.
     *
     * @param id         the project primary key
     * @param requestDto validated input from the controller
     * @return the updated project as a response DTO
     * @throws ResourceNotFoundException if no project exists with the given ID
     */
    public ProjectResponseDto updateProject(Long id, ProjectRequestDto requestDto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id: " + id));

        project.setName(requestDto.getName());
        project.setDescription(requestDto.getDescription());
        project.setGithubUrl(requestDto.getGithubUrl());

        Project updated = projectRepository.save(project);
        return toResponseDto(updated);
    }

    /**
     * Deletes a project by its ID.
     * @param id the project primary key
     * @throws ResourceNotFoundException if no project exists with the given ID
     */
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Project not found with id: " + id);
        }
        projectRepository.deleteById(id);
    }

    // ─── Private helper — Entity → DTO mapping ────────────────────────────────

    /**
     * Maps a Project entity to a ProjectResponseDto.
     * Centralising the mapping here means only this one method needs to change
     * if the DTO shape evolves later.
     */
    private ProjectResponseDto toResponseDto(Project project) {
        return new ProjectResponseDto(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getGithubUrl(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}