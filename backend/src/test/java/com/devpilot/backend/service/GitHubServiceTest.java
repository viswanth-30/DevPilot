package com.devpilot.backend.service;

import com.devpilot.backend.dto.FileContentDto;
import com.devpilot.backend.dto.FileTreeDto;
import com.devpilot.backend.dto.ProjectResponseDto;
import com.devpilot.backend.dto.RepoMetadataDto;
import com.devpilot.backend.exception.GitHubApiException;
import com.devpilot.backend.exception.ResourceNotFoundException;
import com.devpilot.backend.github.GitHubApiClient;
import com.devpilot.backend.model.Project;
import com.devpilot.backend.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GitHubService.
 *
 * No Spring context — just plain Mockito.
 * ProjectRepository and GitHubApiClient are both mocked so no database
 * and no real GitHub API calls are made.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GitHubService")
class GitHubServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private GitHubApiClient gitHubApiClient;

    private GitHubService gitHubService;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Builds a minimal Project entity with a known ID and githubUrl. */
    private Project projectWith(Long id, String githubUrl) {
        Project p = new Project("Test Project", "A test project", githubUrl);
        // Simulate JPA-assigned ID via reflection — avoids a no-arg setter on id
        try {
            var field = Project.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(p, id);
        } catch (Exception ex) {
            throw new RuntimeException("Could not set id via reflection", ex);
        }
        // Simulate @PrePersist — timestamps must be non-null for toResponseDto()
        try {
            var created = Project.class.getDeclaredField("createdAt");
            created.setAccessible(true);
            created.set(p, LocalDateTime.of(2026, 9, 25, 10, 0));
            var updated = Project.class.getDeclaredField("updatedAt");
            updated.setAccessible(true);
            updated.set(p, LocalDateTime.of(2026, 9, 25, 10, 0));
        } catch (Exception ex) {
            throw new RuntimeException("Could not set timestamps via reflection", ex);
        }
        return p;
    }

    /** Builds a RepoMetadataDto as if GitHub returned it. */
    private RepoMetadataDto metadataFor(String name, String defaultBranch) {
        RepoMetadataDto dto = new RepoMetadataDto();
        dto.setName(name);
        dto.setDefaultBranch(defaultBranch);
        RepoMetadataDto.Owner owner = new RepoMetadataDto.Owner();
        owner.setLogin("facebook");
        dto.setOwner(owner);
        return dto;
    }

    @BeforeEach
    void setUp() {
        gitHubService = new GitHubService(projectRepository, gitHubApiClient);
    }

    // ─── Successful connection ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Successful repository connection")
    class SuccessfulConnection {

        @Test
        @DisplayName("persists repoOwner, repoName, and defaultBranch on the project")
        void persistsConnectionMetadata() {
            // Arrange
            Project project = projectWith(1L, "https://github.com/facebook/react");
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(gitHubApiClient.getRepositoryMetadata("facebook", "react"))
                    .thenReturn(metadataFor("react", "main"));
            when(projectRepository.save(any(Project.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            ProjectResponseDto result = gitHubService.connectRepository(1L);

            // Assert — returned DTO
            assertNotNull(result);
            assertEquals("facebook", result.getRepoOwner());
            assertEquals("react", result.getRepoName());
            assertEquals("main", result.getDefaultBranch());
            assertEquals("https://github.com/facebook/react", result.getGithubUrl());

            // Assert — entity was saved with correct fields
            ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
            verify(projectRepository).save(captor.capture());
            Project saved = captor.getValue();
            assertEquals("facebook", saved.getRepoOwner());
            assertEquals("react", saved.getRepoName());
            assertEquals("main", saved.getDefaultBranch());
        }

        @Test
        @DisplayName("calls GitHubApiClient with the correct owner and repo name")
        void callsApiClientWithParsedValues() {
            Project project = projectWith(2L, "https://github.com/spring-projects/spring-boot");
            when(projectRepository.findById(2L)).thenReturn(Optional.of(project));
            when(gitHubApiClient.getRepositoryMetadata("spring-projects", "spring-boot"))
                    .thenReturn(metadataFor("spring-boot", "main"));
            when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            gitHubService.connectRepository(2L);

            verify(gitHubApiClient).getRepositoryMetadata("spring-projects", "spring-boot");
        }

        @Test
        @DisplayName("strips .git suffix from URL before calling API")
        void stripsGitSuffix() {
            Project project = projectWith(3L, "https://github.com/facebook/react.git");
            when(projectRepository.findById(3L)).thenReturn(Optional.of(project));
            when(gitHubApiClient.getRepositoryMetadata("facebook", "react"))
                    .thenReturn(metadataFor("react", "main"));
            when(projectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ProjectResponseDto result = gitHubService.connectRepository(3L);

            verify(gitHubApiClient).getRepositoryMetadata("facebook", "react");
            assertEquals("react", result.getRepoName());
        }
    }

    // ─── Project not found ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Project not found")
    class ProjectNotFound {

        @Test
        @DisplayName("throws ResourceNotFoundException when project ID does not exist")
        void throwsResourceNotFoundException() {
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> gitHubService.connectRepository(99L));

            assertTrue(ex.getMessage().contains("99"));
            verifyNoInteractions(gitHubApiClient);
        }
    }

    // ─── githubUrl missing or blank ────────────────────────────────────────────

    @Nested
    @DisplayName("githubUrl missing or blank")
    class MissingGithubUrl {

        @Test
        @DisplayName("throws GitHubApiException(400) when githubUrl is null")
        void throwsWhenUrlIsNull() {
            Project project = projectWith(4L, null);
            when(projectRepository.findById(4L)).thenReturn(Optional.of(project));

            GitHubApiException ex = assertThrows(
                    GitHubApiException.class,
                    () -> gitHubService.connectRepository(4L));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            verifyNoInteractions(gitHubApiClient);
        }

        @Test
        @DisplayName("throws GitHubApiException(400) when githubUrl is blank")
        void throwsWhenUrlIsBlank() {
            Project project = projectWith(5L, "   ");
            when(projectRepository.findById(5L)).thenReturn(Optional.of(project));

            GitHubApiException ex = assertThrows(
                    GitHubApiException.class,
                    () -> gitHubService.connectRepository(5L));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            verifyNoInteractions(gitHubApiClient);
        }
    }

    // ─── Invalid GitHub URL ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Invalid GitHub URL")
    class InvalidGithubUrl {

        @Test
        @DisplayName("throws GitHubApiException(400) for a non-GitHub URL")
        void throwsForNonGitHubUrl() {
            Project project = projectWith(6L, "https://gitlab.com/owner/repo");
            when(projectRepository.findById(6L)).thenReturn(Optional.of(project));

            GitHubApiException ex = assertThrows(
                    GitHubApiException.class,
                    () -> gitHubService.connectRepository(6L));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("Invalid GitHub URL"));
            verifyNoInteractions(gitHubApiClient);
        }

        @Test
        @DisplayName("throws GitHubApiException(400) for a URL missing the repo name")
        void throwsForUrlWithNoRepo() {
            Project project = projectWith(7L, "https://github.com/owner");
            when(projectRepository.findById(7L)).thenReturn(Optional.of(project));

            GitHubApiException ex = assertThrows(
                    GitHubApiException.class,
                    () -> gitHubService.connectRepository(7L));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            verifyNoInteractions(gitHubApiClient);
        }

        @Test
        @DisplayName("throws GitHubApiException(400) for a subpath URL")
        void throwsForSubpathUrl() {
            Project project = projectWith(8L, "https://github.com/owner/repo/issues");
            when(projectRepository.findById(8L)).thenReturn(Optional.of(project));

            GitHubApiException ex = assertThrows(
                    GitHubApiException.class,
                    () -> gitHubService.connectRepository(8L));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            verifyNoInteractions(gitHubApiClient);
        }
    }

    // ─── GitHubApiException propagation ───────────────────────────────────────

    @Nested
    @DisplayName("GitHubApiException propagation")
    class ApiExceptionPropagation {

        @Test
        @DisplayName("propagates GitHubApiException(404) from GitHubApiClient unchanged")
        void propagates404() {
            Project project = projectWith(10L, "https://github.com/owner/nonexistent");
            when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
            when(gitHubApiClient.getRepositoryMetadata("owner", "nonexistent"))
                    .thenThrow(new GitHubApiException(HttpStatus.NOT_FOUND, "GitHub repository not found."));

            GitHubApiException ex = assertThrows(
                    GitHubApiException.class,
                    () -> gitHubService.connectRepository(10L));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
            assertEquals("GitHub repository not found.", ex.getMessage());
            verify(projectRepository, never()).save(any());
        }

        @Test
        @DisplayName("propagates GitHubApiException(401) from GitHubApiClient unchanged")
        void propagates401() {
            Project project = projectWith(11L, "https://github.com/owner/repo");
            when(projectRepository.findById(11L)).thenReturn(Optional.of(project));
            when(gitHubApiClient.getRepositoryMetadata("owner", "repo"))
                    .thenThrow(new GitHubApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired GitHub token."));

            GitHubApiException ex = assertThrows(
                    GitHubApiException.class,
                    () -> gitHubService.connectRepository(11L));

            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
            verify(projectRepository, never()).save(any());
        }

        @Test
        @DisplayName("propagates GitHubApiException(429) rate limit from GitHubApiClient unchanged")
        void propagates429() {
            Project project = projectWith(12L, "https://github.com/owner/repo");
            when(projectRepository.findById(12L)).thenReturn(Optional.of(project));
            when(gitHubApiClient.getRepositoryMetadata("owner", "repo"))
                    .thenThrow(new GitHubApiException(HttpStatus.TOO_MANY_REQUESTS,
                            "GitHub API rate limit exceeded or access forbidden."));

            GitHubApiException ex = assertThrows(
                    GitHubApiException.class,
                    () -> gitHubService.connectRepository(12L));

            assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
            verify(projectRepository, never()).save(any());
        }

        @Test
        @DisplayName("propagates GitHubApiException(502) network failure from GitHubApiClient unchanged")
        void propagates502() {
            Project project = projectWith(13L, "https://github.com/owner/repo");
            when(projectRepository.findById(13L)).thenReturn(Optional.of(project));
            when(gitHubApiClient.getRepositoryMetadata("owner", "repo"))
                    .thenThrow(new GitHubApiException(HttpStatus.BAD_GATEWAY,
                            "Failed to communicate with GitHub API."));

            GitHubApiException ex = assertThrows(
                    GitHubApiException.class,
                    () -> gitHubService.connectRepository(13L));

            assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
            verify(projectRepository, never()).save(any());
        }

        @Test
        @DisplayName("does NOT save the project when GitHub API throws an exception")
        void doesNotSaveOnApiError() {
            Project project = projectWith(14L, "https://github.com/owner/repo");
            when(projectRepository.findById(14L)).thenReturn(Optional.of(project));
            when(gitHubApiClient.getRepositoryMetadata(any(), any()))
                    .thenThrow(new GitHubApiException(HttpStatus.NOT_FOUND, "GitHub repository not found."));

            assertThrows(GitHubApiException.class,
                    () -> gitHubService.connectRepository(14L));

            verify(projectRepository, never()).save(any());
        }
    }

    // ─── getRepositoryTree() tests ────────────────────────────────────────────

    @Nested
    @DisplayName("getRepositoryTree()")
    class GetRepositoryTree {

        /**
         * Builds a connected project with repoOwner, repoName, and defaultBranch set.
         * Represents the state after a successful POST /api/github/connect/{projectId}.
         */
        private Project connectedProject(Long id) {
            Project p = projectWith(id, "https://github.com/facebook/react");
            p.setRepoOwner("facebook");
            p.setRepoName("react");
            p.setDefaultBranch("main");
            return p;
        }

        /** Builds a minimal FileTreeDto to use as a mock return value. */
        private FileTreeDto sampleTree() {
            FileTreeDto.TreeEntryDto file = new FileTreeDto.TreeEntryDto();
            file.setPath("src/index.js");
            file.setMode("100644");
            file.setType("blob");
            file.setSha("abc123");
            file.setSize(256L);

            FileTreeDto.TreeEntryDto dir = new FileTreeDto.TreeEntryDto();
            dir.setPath("src");
            dir.setMode("040000");
            dir.setType("tree");
            dir.setSha("def456");

            FileTreeDto dto = new FileTreeDto();
            dto.setTruncated(false);
            dto.setTree(java.util.List.of(dir, file));
            return dto;
        }

        @Test
        @DisplayName("A. Returns FileTreeDto when project is connected and GitHub responds successfully")
        void successfulTreeRetrieval() {
            Project project = connectedProject(20L);
            when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
            when(gitHubApiClient.getRepositoryTree("facebook", "react", "main"))
                    .thenReturn(sampleTree());

            FileTreeDto result = gitHubService.getRepositoryTree(20L);

            assertNotNull(result);
            assertFalse(result.isTruncated());
            assertEquals(2, result.getTree().size());

            verify(gitHubApiClient).getRepositoryTree("facebook", "react", "main");
            // getRepositoryTree must never call save — tree is not persisted
            verify(projectRepository, never()).save(any());
        }

        @Test
        @DisplayName("B. Throws ResourceNotFoundException when project does not exist")
        void projectNotFound() {
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> gitHubService.getRepositoryTree(99L));

            assertTrue(ex.getMessage().contains("99"));
            verifyNoInteractions(gitHubApiClient);
        }

        @Test
        @DisplayName("C. Throws GitHubApiException(400) when repoOwner is missing")
        void missingRepoOwner() {
            Project p = projectWith(21L, "https://github.com/facebook/react");
            // repoOwner not set, repoName and defaultBranch also null
            when(projectRepository.findById(21L)).thenReturn(Optional.of(p));

            GitHubApiException ex = assertThrows(
                    GitHubApiException.class,
                    () -> gitHubService.getRepositoryTree(21L));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("not connected"));
            verifyNoInteractions(gitHubApiClient);
        }

        @Test
        @DisplayName("D. Throws GitHubApiException(400) when repoName is missing")
        void missingRepoName() {
            Project p = projectWith(22L, "https://github.com/facebook/react");
            p.setRepoOwner("facebook");
            // repoName not set
            when(projectRepository.findById(22L)).thenReturn(Optional.of(p));

            GitHubApiException ex = assertThrows(
                    GitHubApiException.class,
                    () -> gitHubService.getRepositoryTree(22L));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("not connected"));
            verifyNoInteractions(gitHubApiClient);
        }

        @Test
        @DisplayName("E. Throws GitHubApiException(400) when defaultBranch is missing")
        void missingDefaultBranch() {
            Project p = projectWith(23L, "https://github.com/facebook/react");
            p.setRepoOwner("facebook");
            p.setRepoName("react");
            // defaultBranch not set
            when(projectRepository.findById(23L)).thenReturn(Optional.of(p));

            GitHubApiException ex = assertThrows(
                    GitHubApiException.class,
                    () -> gitHubService.getRepositoryTree(23L));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("not connected"));
            verifyNoInteractions(gitHubApiClient);
        }

        @Test
        @DisplayName("F. Propagates GitHubApiException from GitHubApiClient unchanged")
        void propagatesApiException() {
            Project project = connectedProject(24L);
            when(projectRepository.findById(24L)).thenReturn(Optional.of(project));
            when(gitHubApiClient.getRepositoryTree(eq("facebook"), eq("react"), eq("main")))
                    .thenThrow(new GitHubApiException(HttpStatus.UNAUTHORIZED,
                            "Invalid or expired GitHub token."));

            GitHubApiException ex = assertThrows(
                    GitHubApiException.class,
                    () -> gitHubService.getRepositoryTree(24L));

            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
            assertEquals("Invalid or expired GitHub token.", ex.getMessage());
            verify(projectRepository, never()).save(any());
        }
    }

    // ─── getFileContent() tests ───────────────────────────────────────────────

    @Nested
    @DisplayName("getFileContent()")
    class GetFileContent {

        private Project connectedProject(Long id) {
            Project p = projectWith(id, "https://github.com/facebook/react");
            p.setRepoOwner("facebook");
            p.setRepoName("react");
            p.setDefaultBranch("main");
            return p;
        }

        private FileContentDto sampleContentDto(String content, String encoding) {
            FileContentDto dto = new FileContentDto();
            dto.setName("ProjectService.java");
            dto.setPath("src/main/java/ProjectService.java");
            dto.setEncoding(encoding);
            dto.setContent(content);
            dto.setSha("abc");
            dto.setSize(100L);
            return dto;
        }

        @Test
        @DisplayName("A. Returns decoded FileContentDto when content is base64")
        void successfulRetrieval() {
            Project project = connectedProject(30L);
            when(projectRepository.findById(30L)).thenReturn(Optional.of(project));
            
            // "SGVsbG8gV29ybGQ=" is "Hello World" in base64
            when(gitHubApiClient.getFileContent("facebook", "react", "src/main.java", "main"))
                    .thenReturn(sampleContentDto("SGVsbG8gV29ybGQ=", "base64"));

            FileContentDto result = gitHubService.getFileContent(30L, "src/main.java");

            assertNotNull(result);
            assertEquals("Hello World", result.getContent());
            assertEquals("utf-8", result.getEncoding());

            verify(gitHubApiClient).getFileContent("facebook", "react", "src/main.java", "main");
        }

        @Test
        @DisplayName("B. Throws ResourceNotFoundException when project does not exist")
        void projectNotFound() {
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> gitHubService.getFileContent(99L, "path/to/file"));
            verifyNoInteractions(gitHubApiClient);
        }

        @Test
        @DisplayName("C. Throws GitHubApiException(400) when repoOwner is missing")
        void missingRepoOwner() {
            Project p = projectWith(31L, "url");
            // repoOwner null
            when(projectRepository.findById(31L)).thenReturn(Optional.of(p));

            GitHubApiException ex = assertThrows(GitHubApiException.class,
                    () -> gitHubService.getFileContent(31L, "path/to/file"));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("not connected"));
        }

        @Test
        @DisplayName("D. Throws GitHubApiException(400) when repoName is missing")
        void missingRepoName() {
            Project p = projectWith(32L, "url");
            p.setRepoOwner("owner");
            // repoName null
            when(projectRepository.findById(32L)).thenReturn(Optional.of(p));

            GitHubApiException ex = assertThrows(GitHubApiException.class,
                    () -> gitHubService.getFileContent(32L, "path/to/file"));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("not connected"));
        }

        @Test
        @DisplayName("E. Throws GitHubApiException(400) when defaultBranch is missing")
        void missingDefaultBranch() {
            Project p = projectWith(33L, "url");
            p.setRepoOwner("owner");
            p.setRepoName("repo");
            // defaultBranch null
            when(projectRepository.findById(33L)).thenReturn(Optional.of(p));

            GitHubApiException ex = assertThrows(GitHubApiException.class,
                    () -> gitHubService.getFileContent(33L, "path/to/file"));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("not connected"));
        }

        @Test
        @DisplayName("F. Throws GitHubApiException(400) when path is null")
        void nullPath() {
            GitHubApiException ex = assertThrows(GitHubApiException.class,
                    () -> gitHubService.getFileContent(30L, null));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("null or blank"));
        }

        @Test
        @DisplayName("G. Throws GitHubApiException(400) when path is blank")
        void blankPath() {
            GitHubApiException ex = assertThrows(GitHubApiException.class,
                    () -> gitHubService.getFileContent(30L, "   "));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("null or blank"));
        }

        @Test
        @DisplayName("H/I. Propagates GitHubApiException from GitHubApiClient")
        void propagatesApiException() {
            Project project = connectedProject(34L);
            when(projectRepository.findById(34L)).thenReturn(Optional.of(project));
            when(gitHubApiClient.getFileContent(eq("facebook"), eq("react"), eq("dir"), eq("main")))
                    .thenThrow(new GitHubApiException(HttpStatus.BAD_REQUEST, "Requested path is a directory, not a file."));

            GitHubApiException ex = assertThrows(GitHubApiException.class,
                    () -> gitHubService.getFileContent(34L, "dir"));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertEquals("Requested path is a directory, not a file.", ex.getMessage());
        }

        @Test
        @DisplayName("J. Leaves content as is if not base64 encoded")
        void leavesContentUnchangedIfNotBase64() {
            Project project = connectedProject(35L);
            when(projectRepository.findById(35L)).thenReturn(Optional.of(project));
            
            when(gitHubApiClient.getFileContent("facebook", "react", "src/main.java", "main"))
                    .thenReturn(sampleContentDto("raw text content", "utf-8"));

            FileContentDto result = gitHubService.getFileContent(35L, "src/main.java");

            assertNotNull(result);
            assertEquals("raw text content", result.getContent());
            assertEquals("utf-8", result.getEncoding());
        }
    }
}
