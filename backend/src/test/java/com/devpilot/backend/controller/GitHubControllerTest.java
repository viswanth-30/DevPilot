package com.devpilot.backend.controller;

import com.devpilot.backend.dto.FileContentDto;
import com.devpilot.backend.dto.FileTreeDto;
import com.devpilot.backend.dto.ProjectResponseDto;
import com.devpilot.backend.exception.GitHubApiException;
import com.devpilot.backend.exception.GlobalExceptionHandler;
import com.devpilot.backend.exception.ResourceNotFoundException;
import com.devpilot.backend.service.GitHubService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer tests for GitHubController.
 *
 * Uses @WebMvcTest to load only the web layer (no JPA, no DB).
 * GitHubService is mocked — no real service logic runs.
 * GlobalExceptionHandler is included automatically in @WebMvcTest context.
 */
@WebMvcTest(controllers = {GitHubController.class, GlobalExceptionHandler.class})
@DisplayName("GitHubController")
class GitHubControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GitHubService gitHubService;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ProjectResponseDto connectedResponse() {
        return new ProjectResponseDto(
                1L,
                "My Project",
                "A project",
                "https://github.com/facebook/react",
                "facebook",
                "react",
                "main",
                LocalDateTime.of(2026, 9, 25, 10, 0),
                LocalDateTime.of(2026, 9, 25, 10, 0)
        );
    }

    // ─── Successful connection ─────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/github/connect/{projectId}")
    class ConnectRepository {

        @Test
        @DisplayName("returns 200 OK with updated project when connection succeeds")
        void successReturns200() throws Exception {
            when(gitHubService.connectRepository(1L)).thenReturn(connectedResponse());

            mockMvc.perform(post("/api/github/connect/1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.repoOwner").value("facebook"))
                    .andExpect(jsonPath("$.repoName").value("react"))
                    .andExpect(jsonPath("$.defaultBranch").value("main"))
                    .andExpect(jsonPath("$.githubUrl").value("https://github.com/facebook/react"));
        }

        @Test
        @DisplayName("returns 404 when project does not exist")
        void projectNotFoundReturns404() throws Exception {
            when(gitHubService.connectRepository(99L))
                    .thenThrow(new ResourceNotFoundException("Project not found with id: 99"));

            mockMvc.perform(post("/api/github/connect/99")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Project not found with id: 99"));
        }

        @Test
        @DisplayName("returns 400 when githubUrl is missing on the project")
        void missingUrlReturns400() throws Exception {
            when(gitHubService.connectRepository(2L))
                    .thenThrow(new GitHubApiException(
                            HttpStatus.BAD_REQUEST,
                            "Project does not have a GitHub URL set."));

            mockMvc.perform(post("/api/github/connect/2")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("returns 400 when githubUrl is invalid")
        void invalidUrlReturns400() throws Exception {
            when(gitHubService.connectRepository(3L))
                    .thenThrow(new GitHubApiException(
                            HttpStatus.BAD_REQUEST,
                            "Invalid GitHub URL: Invalid GitHub URL — host must be 'github.com'"));

            mockMvc.perform(post("/api/github/connect/3")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(
                            "Invalid GitHub URL: Invalid GitHub URL — host must be 'github.com'"));
        }

        @Test
        @DisplayName("returns 404 when GitHub repository does not exist")
        void githubRepoNotFoundReturns404() throws Exception {
            when(gitHubService.connectRepository(4L))
                    .thenThrow(new GitHubApiException(
                            HttpStatus.NOT_FOUND, "GitHub repository not found."));

            mockMvc.perform(post("/api/github/connect/4")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("GitHub repository not found."));
        }

        @Test
        @DisplayName("returns 401 when GitHub token is invalid")
        void unauthorizedReturns401() throws Exception {
            when(gitHubService.connectRepository(5L))
                    .thenThrow(new GitHubApiException(
                            HttpStatus.UNAUTHORIZED, "Invalid or expired GitHub token."));

            mockMvc.perform(post("/api/github/connect/5")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("returns 429 when GitHub API rate limit exceeded")
        void rateLimitReturns429() throws Exception {
            when(gitHubService.connectRepository(6L))
                    .thenThrow(new GitHubApiException(
                            HttpStatus.TOO_MANY_REQUESTS,
                            "GitHub API rate limit exceeded or access forbidden."));

            mockMvc.perform(post("/api/github/connect/6")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.status").value(429));
        }

        @Test
        @DisplayName("returns 502 when GitHub API is unavailable")
        void gatewayErrorReturns502() throws Exception {
            when(gitHubService.connectRepository(7L))
                    .thenThrow(new GitHubApiException(
                            HttpStatus.BAD_GATEWAY, "GitHub API is unavailable."));

            mockMvc.perform(post("/api/github/connect/7")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.status").value(502));
        }
    }

    // ─── GET /api/github/tree/{projectId} tests ──────────────────────────────

    @Nested
    @DisplayName("GET /api/github/tree/{projectId}")
    class GetRepositoryTree {

        /** Builds a sample FileTreeDto with two entries for use in success assertions. */
        private FileTreeDto sampleTree() {
            FileTreeDto.TreeEntryDto dir = new FileTreeDto.TreeEntryDto();
            dir.setPath("src");
            dir.setMode("040000");
            dir.setType("tree");
            dir.setSha("def456");

            FileTreeDto.TreeEntryDto file = new FileTreeDto.TreeEntryDto();
            file.setPath("src/index.js");
            file.setMode("100644");
            file.setType("blob");
            file.setSha("abc123");
            file.setSize(256L);

            FileTreeDto dto = new FileTreeDto();
            dto.setTruncated(false);
            dto.setTree(List.of(dir, file));
            return dto;
        }

        @Test
        @DisplayName("A. Returns 200 OK with FileTreeDto when repository is connected")
        void successReturns200() throws Exception {
            when(gitHubService.getRepositoryTree(1L)).thenReturn(sampleTree());

            mockMvc.perform(get("/api/github/tree/1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.truncated").value(false))
                    .andExpect(jsonPath("$.tree").isArray())
                    .andExpect(jsonPath("$.tree.length()").value(2))
                    .andExpect(jsonPath("$.tree[0].path").value("src"))
                    .andExpect(jsonPath("$.tree[0].type").value("tree"))
                    .andExpect(jsonPath("$.tree[1].path").value("src/index.js"))
                    .andExpect(jsonPath("$.tree[1].type").value("blob"))
                    .andExpect(jsonPath("$.tree[1].sha").value("abc123"));
        }

        @Test
        @DisplayName("B. Returns 404 when project does not exist")
        void projectNotFoundReturns404() throws Exception {
            when(gitHubService.getRepositoryTree(99L))
                    .thenThrow(new ResourceNotFoundException("Project not found with id: 99"));

            mockMvc.perform(get("/api/github/tree/99")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Project not found with id: 99"));
        }

        @Test
        @DisplayName("C. Returns 400 when project is not connected (missing connection metadata)")
        void notConnectedReturns400() throws Exception {
            when(gitHubService.getRepositoryTree(2L))
                    .thenThrow(new GitHubApiException(
                            HttpStatus.BAD_REQUEST,
                            "Project repository is not connected. " +
                            "Call POST /api/github/connect/2 first."));

            mockMvc.perform(get("/api/github/tree/2")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value(
                            "Project repository is not connected. " +
                            "Call POST /api/github/connect/2 first."));
        }
    }

    // ─── GET /api/github/file/{projectId} tests ──────────────────────────────

    @Nested
    @DisplayName("GET /api/github/file/{projectId}")
    class GetFileContent {

        private FileContentDto sampleContentDto() {
            FileContentDto dto = new FileContentDto();
            dto.setName("Main.java");
            dto.setPath("src/Main.java");
            dto.setSha("abc");
            dto.setSize(100L);
            dto.setEncoding("utf-8");
            dto.setContent("public class Main {}");
            dto.setHtmlUrl("https://github.com/facebook/react/blob/main/src/Main.java");
            return dto;
        }

        @Test
        @DisplayName("A. Returns 200 OK with FileContentDto on successful request")
        void successReturns200() throws Exception {
            when(gitHubService.getFileContent(1L, "src/Main.java"))
                    .thenReturn(sampleContentDto());

            mockMvc.perform(get("/api/github/file/1")
                            .param("path", "src/Main.java")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.name").value("Main.java"))
                    .andExpect(jsonPath("$.path").value("src/Main.java"))
                    .andExpect(jsonPath("$.encoding").value("utf-8"))
                    .andExpect(jsonPath("$.content").value("public class Main {}"));
        }

        @Test
        @DisplayName("B. Returns 400 when path parameter is missing")
        void missingPathReturns400() throws Exception {
            // Missing '?path=...'
            mockMvc.perform(get("/api/github/file/1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    // Spring MVC throws MissingServletRequestParameterException, which maps to 400
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("C. Returns 404 when project does not exist")
        void projectNotFoundReturns404() throws Exception {
            when(gitHubService.getFileContent(99L, "src/Main.java"))
                    .thenThrow(new ResourceNotFoundException("Project not found with id: 99"));

            mockMvc.perform(get("/api/github/file/99")
                            .param("path", "src/Main.java")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Project not found with id: 99"));
        }

        @Test
        @DisplayName("D. Returns 400 when request is for a directory (non-file request)")
        void directoryReturns400() throws Exception {
            when(gitHubService.getFileContent(2L, "src"))
                    .thenThrow(new GitHubApiException(HttpStatus.BAD_REQUEST, "Requested path is a directory, not a file."));

            mockMvc.perform(get("/api/github/file/2")
                            .param("path", "src")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("Requested path is a directory, not a file."));
        }
    }
}
