package com.devpilot.backend.controller;

import com.devpilot.backend.dto.FileContentDto;
import com.devpilot.backend.dto.FileTreeDto;
import com.devpilot.backend.dto.ProjectResponseDto;
import com.devpilot.backend.service.GitHubService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that exposes the GitHub integration API.
 *
 * Deliberately kept separate from ProjectController so that each
 * controller has a single responsibility:
 *   - ProjectController  → Project CRUD
 *   - GitHubController   → GitHub repository operations
 *
 * All error handling (404, 400, 401, 403, 429, 502) is delegated to
 * GlobalExceptionHandler — no try/catch blocks are needed here.
 *
 * Constructor injection is used for all dependencies.
 */
@RestController
@RequestMapping("/api/github")
public class GitHubController {

    private final GitHubService gitHubService;

    public GitHubController(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    /**
     * POST /api/github/connect/{projectId}
     *
     * Connects a DevPilot project to its GitHub repository.
     * Reads the githubUrl already stored on the project — no request body needed.
     *
     * Successful response: HTTP 200 OK with the updated ProjectResponseDto.
     * The response will include repoOwner, repoName, and defaultBranch
     * populated from the GitHub API.
     *
     * Error responses (handled by GlobalExceptionHandler):
     *   404  — project does not exist
     *   400  — githubUrl is missing or not a valid GitHub repository URL
     *   401  — GitHub token is invalid or expired
     *   403/429 — GitHub API rate limit exceeded or access forbidden
     *   404  — GitHub repository not found
     *   502  — GitHub API unavailable or network failure
     */
    @PostMapping("/connect/{projectId}")
    public ResponseEntity<ProjectResponseDto> connectRepository(
            @PathVariable Long projectId) {

        ProjectResponseDto response = gitHubService.connectRepository(projectId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/github/tree/{projectId}
     *
     * Returns the complete recursive file tree of a connected GitHub repository.
     *
     * The project must have been previously connected via
     * POST /api/github/connect/{projectId}, which populates repoOwner, repoName,
     * and defaultBranch on the project record. This endpoint reads those fields
     * directly — no URL re-parsing.
     *
     * Successful response: HTTP 200 OK with a FileTreeDto body containing:
     *   - truncated: true if GitHub's response was incomplete (very large repos)
     *   - tree: list of TreeEntryDto (path, mode, type, sha, size, url)
     *
     * Error responses (handled by GlobalExceptionHandler):
     *   404  — project does not exist
     *   400  — project has not been connected (missing repoOwner/repoName/defaultBranch)
     *   401  — GitHub token is invalid or expired
     *   403/429 — GitHub API rate limit exceeded or access forbidden
     *   404  — GitHub repository tree not found
     *   502  — GitHub API unavailable or network failure
     */
    @GetMapping("/tree/{projectId}")
    public ResponseEntity<FileTreeDto> getRepositoryTree(
            @PathVariable Long projectId) {

        FileTreeDto tree = gitHubService.getRepositoryTree(projectId);
        return ResponseEntity.ok(tree);
    }
    /**
     * GET /api/github/file/{projectId}?path=<file-path>
     *
     * Retrieves the decoded contents of an individual file from a connected GitHub repository.
     *
     * Successful response: HTTP 200 OK with a FileContentDto body containing:
     *   - name, path, sha, size, html_url
     *   - content: the decoded raw source code of the file
     *   - encoding: "utf-8"
     *
     * Error responses (handled by GlobalExceptionHandler):
     *   400  — path is missing/blank, project not connected, or path is a directory
     *   404  — project does not exist, or file not found in GitHub
     *   401  — GitHub token is invalid or expired
     *   403/429 — GitHub API rate limit exceeded or access forbidden
     *   502  — GitHub API unavailable or network failure
     */
    @GetMapping("/file/{projectId}")
    public ResponseEntity<FileContentDto> getFileContent(
            @PathVariable Long projectId,
            @RequestParam("path") String path) {

        FileContentDto fileContent = gitHubService.getFileContent(projectId, path);
        return ResponseEntity.ok(fileContent);
    }
}
