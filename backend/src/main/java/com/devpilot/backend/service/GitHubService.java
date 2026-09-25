package com.devpilot.backend.service;

import com.devpilot.backend.dto.FileContentDto;
import com.devpilot.backend.dto.FileTreeDto;
import com.devpilot.backend.dto.ProjectResponseDto;
import com.devpilot.backend.dto.RepoMetadataDto;
import com.devpilot.backend.exception.GitHubApiException;
import com.devpilot.backend.exception.ResourceNotFoundException;
import com.devpilot.backend.github.GitHubApiClient;
import com.devpilot.backend.github.GitHubUrlParser;
import com.devpilot.backend.github.GitHubUrlParser.ParsedGitHubUrl;
import com.devpilot.backend.model.Project;
import com.devpilot.backend.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Service for GitHub repository operations.
 *
 * Responsibilities:
 *   - Load Projects from the database (delegating to ProjectRepository).
 *   - Validate project state before calling GitHub APIs.
 *   - Coordinate between GitHubUrlParser and GitHubApiClient.
 *   - Persist connection metadata when a repository is connected.
 *   - Return appropriate DTOs to the controller layer.
 *
 * This service does NOT contain any direct HTTP logic — GitHubApiClient
 * is solely responsible for all GitHub API communication.
 *
 * Constructor injection is used for all dependencies.
 */
@Service
public class GitHubService {

    private final ProjectRepository projectRepository;
    private final GitHubApiClient gitHubApiClient;

    public GitHubService(ProjectRepository projectRepository,
                         GitHubApiClient gitHubApiClient) {
        this.projectRepository = projectRepository;
        this.gitHubApiClient = gitHubApiClient;
    }

    /**
     * Connects a DevPilot project to its GitHub repository.
     *
     * Reads the githubUrl already stored on the project, validates it,
     * calls the GitHub REST API to confirm the repository exists, then
     * stores the parsed owner, repo name, and default branch on the project.
     *
     * @param projectId the primary key of the project to connect
     * @return the updated project as a response DTO
     * @throws ResourceNotFoundException if no project exists with the given ID
     * @throws GitHubApiException        if the URL is missing/invalid, or any
     *                                   GitHub API error occurs
     */
    public ProjectResponseDto connectRepository(Long projectId) {

        // ── 1. Load project — 404 if it does not exist ────────────────────────
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id: " + projectId));

        // ── 2. Validate that a GitHub URL has been set ─────────────────────────
        String githubUrl = project.getGithubUrl();
        if (githubUrl == null || githubUrl.isBlank()) {
            throw new GitHubApiException(
                    HttpStatus.BAD_REQUEST,
                    "Project does not have a GitHub URL set. " +
                    "Update the project with a valid githubUrl before connecting.");
        }

        // ── 3. Parse the URL to extract owner and repo name ────────────────────
        //      GitHubUrlParser throws IllegalArgumentException on any parse error.
        //      We wrap it into GitHubApiException(400) so the GlobalExceptionHandler
        //      can return a clean, structured error response — no stack trace exposed.
        ParsedGitHubUrl parsed;
        try {
            parsed = GitHubUrlParser.parse(githubUrl);
        } catch (IllegalArgumentException e) {
            throw new GitHubApiException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid GitHub URL: " + e.getMessage());
        }

        // ── 4. Call GitHub API to validate the repository exists ──────────────
        //      GitHubApiClient throws GitHubApiException on HTTP or network errors;
        //      those propagate directly — no duplication of error mapping here.
        RepoMetadataDto metadata = gitHubApiClient.getRepositoryMetadata(
                parsed.getOwner(), parsed.getRepoName());

        // ── 5. Persist the connection metadata on the Project ─────────────────
        project.setRepoOwner(parsed.getOwner());
        project.setRepoName(parsed.getRepoName());
        project.setDefaultBranch(metadata.getDefaultBranch());

        Project saved = projectRepository.save(project);

        // ── 6. Return the updated project as a DTO ────────────────────────────
        return toResponseDto(saved);
    }

    /**
     * Retrieves the complete recursive file tree of a connected GitHub repository.
     *
     * Reads the repoOwner, repoName, and defaultBranch from the project record.
     * These are set during {@link #connectRepository(Long)} and must be present
     * before this method can succeed — avoids redundant URL re-parsing.
     *
     * The returned FileTreeDto contains a {@code truncated} flag that is preserved
     * verbatim from GitHub's response. If true, the tree is incomplete (the
     * repository is too large for a single API response) and the caller must be
     * aware.
     *
     * @param projectId the primary key of the project whose tree is requested
     * @return FileTreeDto containing the file tree and the truncated flag
     * @throws ResourceNotFoundException if no project exists with the given ID
     * @throws GitHubApiException        if connection metadata is missing (400),
     *                                   or any GitHub API error occurs
     */
    public FileTreeDto getRepositoryTree(Long projectId) {

        // ── 1. Load project — 404 if it does not exist ────────────────────────
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id: " + projectId));

        // ── 2. Verify the project has been connected ───────────────────────────
        //      All three fields must be present. If any are missing, the project has
        //      not yet been connected via POST /api/github/connect/{projectId}.
        String repoOwner = project.getRepoOwner();
        String repoName = project.getRepoName();
        String defaultBranch = project.getDefaultBranch();

        if (repoOwner == null || repoOwner.isBlank()
                || repoName == null || repoName.isBlank()
                || defaultBranch == null || defaultBranch.isBlank()) {
            throw new GitHubApiException(
                    HttpStatus.BAD_REQUEST,
                    "Project repository is not connected. " +
                    "Call POST /api/github/connect/" + projectId + " first.");
        }

        // ── 3. Fetch the file tree from GitHub ────────────────────────────────
        //      GitHubApiClient throws GitHubApiException on HTTP or network errors;
        //      those propagate directly without duplication.
        return gitHubApiClient.getRepositoryTree(repoOwner, repoName, defaultBranch);
    }

    /**
     * Retrieves the contents of a specific file from a connected GitHub repository.
     *
     * Validates that the project is connected and the requested path is provided.
     * Fetches the file content using the GitHubApiClient. If the content is Base64
     * encoded (as is standard for the GitHub Contents API), this method decodes it
     * into a UTF-8 string before returning it to ensure the client receives raw
     * source code, not Base64.
     *
     * @param projectId the primary key of the project
     * @param path      the path to the file within the repository (e.g., "src/Main.java")
     * @return FileContentDto containing file metadata and decoded source code
     * @throws ResourceNotFoundException if no project exists with the given ID
     * @throws GitHubApiException        if the path is blank, connection metadata is missing,
     *                                   or any GitHub API error occurs
     */
    public FileContentDto getFileContent(Long projectId, String path) {

        if (path == null || path.isBlank()) {
            throw new GitHubApiException(HttpStatus.BAD_REQUEST, "File path must not be null or blank.");
        }

        // ── 1. Load project — 404 if it does not exist ────────────────────────
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id: " + projectId));

        // ── 2. Verify the project has been connected ───────────────────────────
        String repoOwner = project.getRepoOwner();
        String repoName = project.getRepoName();
        String defaultBranch = project.getDefaultBranch();

        if (repoOwner == null || repoOwner.isBlank()
                || repoName == null || repoName.isBlank()
                || defaultBranch == null || defaultBranch.isBlank()) {
            throw new GitHubApiException(
                    HttpStatus.BAD_REQUEST,
                    "Project repository is not connected. " +
                    "Call POST /api/github/connect/" + projectId + " first.");
        }

        // ── 3. Fetch file content from GitHub API ─────────────────────────────
        FileContentDto fileContent = gitHubApiClient.getFileContent(repoOwner, repoName, path, defaultBranch);

        // ── 4. Decode Base64 content to raw source code ───────────────────────
        if ("base64".equals(fileContent.getEncoding()) && fileContent.getContent() != null) {
            // GitHub base64 can contain newlines, so we use MIME decoder
            byte[] decodedBytes = java.util.Base64.getMimeDecoder().decode(fileContent.getContent());
            fileContent.setContent(new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8));
            fileContent.setEncoding("utf-8"); // Update encoding to reflect we decoded it
        }

        return fileContent;
    }

    // ─── Private helper — Entity → DTO mapping ────────────────────────────────

    private ProjectResponseDto toResponseDto(Project project) {
        return new ProjectResponseDto(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getGithubUrl(),
                project.getRepoOwner(),
                project.getRepoName(),
                project.getDefaultBranch(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
