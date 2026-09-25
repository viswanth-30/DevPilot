package com.devpilot.backend.github;

import com.devpilot.backend.dto.FileContentDto;
import com.devpilot.backend.dto.FileTreeDto;
import com.devpilot.backend.dto.RepoMetadataDto;
import com.devpilot.backend.exception.GitHubApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GitHubApiClient")
class GitHubApiClientTest {

    private GitHubApiClient githubApiClient;
    private ExchangeFunction exchangeFunction;

    @BeforeEach
    void setUp() {
        // We mock the ExchangeFunction to intercept the HTTP request made by WebClient
        exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.OK).build());

        ExchangeFunction mockExchangeFunction = request -> exchangeFunction.exchange(request);

        WebClient.Builder webClientBuilder = WebClient.builder().exchangeFunction(mockExchangeFunction);

        githubApiClient = new GitHubApiClient(
                webClientBuilder,
                "https://api.github.test",
                "test-token-123"
        );
    }

    // ─── Tests for getRepositoryMetadata() ─────────────────────────────────────

    @Nested
    @DisplayName("getRepositoryMetadata()")
    class GetRepositoryMetadata {

        @Test
        @DisplayName("Returns metadata on successful response (200 OK)")
        void successResponse() {
            String jsonResponse = """
                    {
                        "name": "react",
                        "full_name": "facebook/react",
                        "html_url": "https://github.com/facebook/react",
                        "default_branch": "main",
                        "private": false,
                        "owner": {
                            "login": "facebook"
                        }
                    }
                    """;

            exchangeFunction = request -> {
                // Verify Authorization header
                assertEquals("Bearer test-token-123", request.headers().getFirst(HttpHeaders.AUTHORIZATION));
                assertEquals("application/vnd.github.v3+json", request.headers().getFirst(HttpHeaders.ACCEPT));

                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .body(jsonResponse)
                        .build());
            };

            RepoMetadataDto dto = githubApiClient.getRepositoryMetadata("facebook", "react");

            assertNotNull(dto);
            assertEquals("react", dto.getName());
            assertEquals("facebook/react", dto.getFullName());
            assertEquals("https://github.com/facebook/react", dto.getHtmlUrl());
            assertEquals("main", dto.getDefaultBranch());
            assertEquals(false, dto.getPrivate());
            assertNotNull(dto.getOwner());
            assertEquals("facebook", dto.getOwnerLogin());
        }

        @Test
        @DisplayName("Throws GitHubApiException (401) on invalid token")
        void unauthorized() {
            exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED).build());

            GitHubApiException exception = assertThrows(GitHubApiException.class, () ->
                    githubApiClient.getRepositoryMetadata("facebook", "react"));

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
            assertEquals("Invalid or expired GitHub token.", exception.getMessage());
            assertFalse(exception.getMessage().contains("test-token-123"), "Token should not be exposed in error message");
        }

        @Test
        @DisplayName("Throws GitHubApiException (403) on forbidden or rate limit")
        void forbidden() {
            exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.FORBIDDEN).build());

            GitHubApiException exception = assertThrows(GitHubApiException.class, () ->
                    githubApiClient.getRepositoryMetadata("facebook", "react"));

            assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatus());
            assertEquals("GitHub API rate limit exceeded or access forbidden.", exception.getMessage());
        }

        @Test
        @DisplayName("Throws GitHubApiException (404) when repository not found")
        void notFound() {
            exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());

            GitHubApiException exception = assertThrows(GitHubApiException.class, () ->
                    githubApiClient.getRepositoryMetadata("not-exist", "repo"));

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
            assertEquals("GitHub repository not found.", exception.getMessage());
        }

        @Test
        @DisplayName("Throws GitHubApiException (429) on explicit rate limit")
        void tooManyRequests() {
            exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.TOO_MANY_REQUESTS).build());

            GitHubApiException exception = assertThrows(GitHubApiException.class, () ->
                    githubApiClient.getRepositoryMetadata("facebook", "react"));

            assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatus());
            assertEquals("GitHub API rate limit exceeded or access forbidden.", exception.getMessage());
        }

        @Test
        @DisplayName("Throws GitHubApiException (502) on 5xx server errors")
        void serverError() {
            exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build());

            GitHubApiException exception = assertThrows(GitHubApiException.class, () ->
                    githubApiClient.getRepositoryMetadata("facebook", "react"));

            assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
            assertEquals("GitHub API is unavailable.", exception.getMessage());
        }

        @Test
        @DisplayName("Throws GitHubApiException (502) on network failure/timeout")
        void networkFailure() {
            exchangeFunction = request -> Mono.error(new RuntimeException("Connection timed out"));

            GitHubApiException exception = assertThrows(GitHubApiException.class, () ->
                    githubApiClient.getRepositoryMetadata("facebook", "react"));

            assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
            assertEquals("Failed to communicate with GitHub API.", exception.getMessage());
        }
    }

    // ─── Tests for getRepositoryTree() ────────────────────────────────────────

    @Nested
    @DisplayName("getRepositoryTree()")
    class GetRepositoryTree {

        /** Builds a minimal GitHub Git Trees API JSON response. */
        private String treeJson(boolean truncated, String... entries) {
            StringBuilder sb = new StringBuilder();
            sb.append("{ \"truncated\": ").append(truncated).append(", \"tree\": [");
            sb.append(String.join(", ", entries));
            sb.append("] }");
            return sb.toString();
        }

        /** JSON for a regular file entry (blob). */
        private String fileEntry(String path) {
            return """
                    {
                        "path": "%s",
                        "mode": "100644",
                        "type": "blob",
                        "sha": "abc123def456",
                        "size": 512,
                        "url": "https://api.github.test/repos/facebook/react/git/blobs/abc123def456"
                    }
                    """.formatted(path);
        }

        /** JSON for a directory entry (tree). Size is absent — trees have no size. */
        private String dirEntry(String path) {
            return """
                    {
                        "path": "%s",
                        "mode": "040000",
                        "type": "tree",
                        "sha": "def456abc789",
                        "url": "https://api.github.test/repos/facebook/react/git/trees/def456abc789"
                    }
                    """.formatted(path);
        }

        @Test
        @DisplayName("A. Verifies request path, owner/repo/branch, ?recursive=1, and auth header")
        void verifiesRequestDetails() {
            String json = treeJson(false, fileEntry("README.md"));

            exchangeFunction = request -> {
                assertEquals("Bearer test-token-123", request.headers().getFirst(HttpHeaders.AUTHORIZATION));
                assertEquals("application/vnd.github.v3+json", request.headers().getFirst(HttpHeaders.ACCEPT));

                String uri = request.url().toString();
                assertTrue(uri.contains("/repos/facebook/react/git/trees/main"),
                        "URI should contain /repos/facebook/react/git/trees/main, was: " + uri);
                assertTrue(uri.contains("recursive=1"),
                        "URI should contain recursive=1, was: " + uri);

                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .body(json)
                        .build());
            };

            FileTreeDto result = githubApiClient.getRepositoryTree("facebook", "react", "main");

            assertNotNull(result);
            assertFalse(result.isTruncated());
            assertNotNull(result.getTree());
            assertEquals(1, result.getTree().size());
        }

        @Test
        @DisplayName("A. Maps successful response — verifies all TreeEntryDto fields")
        void successfulResponseMapping() {
            String json = treeJson(false, fileEntry("src/index.js"));

            exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(json)
                    .build());

            FileTreeDto result = githubApiClient.getRepositoryTree("facebook", "react", "main");

            assertNotNull(result);
            assertFalse(result.isTruncated());
            assertEquals(1, result.getTree().size());

            FileTreeDto.TreeEntryDto entry = result.getTree().get(0);
            assertEquals("src/index.js", entry.getPath());
            assertEquals("100644", entry.getMode());
            assertEquals("blob", entry.getType());
            assertEquals("abc123def456", entry.getSha());
            assertEquals(512L, entry.getSize());
            assertNotNull(entry.getUrl());
        }

        @Test
        @DisplayName("B. Empty tree — returns FileTreeDto with an empty list, not null")
        void emptyTree() {
            String json = treeJson(false);

            exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(json)
                    .build());

            FileTreeDto result = githubApiClient.getRepositoryTree("empty", "repo", "main");

            assertNotNull(result);
            assertFalse(result.isTruncated());
            assertNotNull(result.getTree());
            assertTrue(result.getTree().isEmpty());
        }

        @Test
        @DisplayName("C. Multiple files and directories — types and null-size for dirs verified")
        void multipleEntries() {
            String json = treeJson(false,
                    dirEntry("src"),
                    fileEntry("src/index.js"),
                    fileEntry("src/App.js"),
                    dirEntry("src/components"),
                    fileEntry("src/components/Button.js"),
                    fileEntry("README.md"));

            exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(json)
                    .build());

            FileTreeDto result = githubApiClient.getRepositoryTree("facebook", "react", "main");

            assertNotNull(result);
            assertEquals(6, result.getTree().size());

            long blobs = result.getTree().stream().filter(e -> "blob".equals(e.getType())).count();
            long trees = result.getTree().stream().filter(e -> "tree".equals(e.getType())).count();
            assertEquals(4, blobs, "4 file entries expected");
            assertEquals(2, trees, "2 directory entries expected");

            // Directory entries must have null size
            result.getTree().stream()
                    .filter(e -> "tree".equals(e.getType()))
                    .forEach(e -> assertNull(e.getSize(), "Directory entries should have null size"));
        }

        @Test
        @DisplayName("D. truncated=true is preserved in FileTreeDto (NOT silently ignored)")
        void truncatedIsPreserved() {
            String json = treeJson(true, fileEntry("file1.js"), fileEntry("file2.js"));

            exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(json)
                    .build());

            FileTreeDto result = githubApiClient.getRepositoryTree("facebook", "react", "main");

            assertNotNull(result);
            assertTrue(result.isTruncated(), "truncated flag must be preserved as true");
            assertEquals(2, result.getTree().size());
        }

        @Test
        @DisplayName("E. Throws GitHubApiException (401) on invalid or expired token")
        void unauthorized() {
            exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED).build());

            GitHubApiException ex = assertThrows(GitHubApiException.class, () ->
                    githubApiClient.getRepositoryTree("facebook", "react", "main"));

            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
            assertEquals("Invalid or expired GitHub token.", ex.getMessage());
        }

        @Test
        @DisplayName("F. Throws GitHubApiException (429) on 403 forbidden / rate limit")
        void forbidden() {
            exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.FORBIDDEN).build());

            GitHubApiException ex = assertThrows(GitHubApiException.class, () ->
                    githubApiClient.getRepositoryTree("facebook", "react", "main"));

            assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
            assertEquals("GitHub API rate limit exceeded or access forbidden.", ex.getMessage());
        }

        @Test
        @DisplayName("G. Throws GitHubApiException (404) when tree or branch not found")
        void notFound() {
            exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());

            GitHubApiException ex = assertThrows(GitHubApiException.class, () ->
                    githubApiClient.getRepositoryTree("facebook", "react", "nonexistent-branch"));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
            assertEquals("GitHub repository tree not found.", ex.getMessage());
        }

        @Test
        @DisplayName("H. Throws GitHubApiException (429) on explicit 429 rate limit response")
        void tooManyRequests() {
            exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.TOO_MANY_REQUESTS).build());

            GitHubApiException ex = assertThrows(GitHubApiException.class, () ->
                    githubApiClient.getRepositoryTree("facebook", "react", "main"));

            assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        }

        @Test
        @DisplayName("I. Throws GitHubApiException (502) on 5xx server error")
        void serverError() {
            exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build());

            GitHubApiException ex = assertThrows(GitHubApiException.class, () ->
                    githubApiClient.getRepositoryTree("facebook", "react", "main"));

            assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
            assertEquals("GitHub API is unavailable.", ex.getMessage());
        }

        @Test
        @DisplayName("J. Throws GitHubApiException (502) on network failure / connection refused")
        void networkFailure() {
            exchangeFunction = request -> Mono.error(new RuntimeException("Connection refused"));

            GitHubApiException ex = assertThrows(GitHubApiException.class, () ->
                    githubApiClient.getRepositoryTree("facebook", "react", "main"));

            assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
            assertEquals("Failed to communicate with GitHub API.", ex.getMessage());
        }
    }

    // ─── Tests for getFileContent() ───────────────────────────────────────────

    @Nested
    @DisplayName("getFileContent()")
    class GetFileContent {

        private String fileContentJson(String content, String encoding) {
            return """
                    {
                        "name": "ProjectService.java",
                        "path": "src/main/java/ProjectService.java",
                        "sha": "abc123def456",
                        "size": 1024,
                        "encoding": "%s",
                        "content": "%s",
                        "html_url": "https://github.com/facebook/react/blob/main/src/main/java/ProjectService.java"
                    }
                    """.formatted(encoding, content.replace("\n", "\\n"));
        }

        @Test
        @DisplayName("A. Verifies request path, branch ref, Authorization, and Accept headers")
        void verifiesRequestDetails() {
            String json = fileContentJson("base64data", "base64");

            exchangeFunction = request -> {
                assertEquals("Bearer test-token-123", request.headers().getFirst(HttpHeaders.AUTHORIZATION));
                assertEquals("application/vnd.github.v3+json", request.headers().getFirst(HttpHeaders.ACCEPT));

                String uri = request.url().toString();
                assertTrue(uri.contains("/repos/facebook/react/contents/src%2Fmain%2Fjava%2FProjectService.java") || 
                           uri.contains("/repos/facebook/react/contents/src/main/java/ProjectService.java"),
                        "URI should contain correct path, was: " + uri);
                assertTrue(uri.contains("ref=main"),
                        "URI should contain ref=main, was: " + uri);

                return Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .body(json)
                        .build());
            };

            FileContentDto result = githubApiClient.getFileContent("facebook", "react", "src/main/java/ProjectService.java", "main");

            assertNotNull(result);
            assertEquals("ProjectService.java", result.getName());
            assertEquals("src/main/java/ProjectService.java", result.getPath());
            assertEquals("base64", result.getEncoding());
            assertEquals("base64data", result.getContent());
        }

        @Test
        @DisplayName("B/C. Base64 content is returned correctly by GitHub")
        void successfulResponseMapping() {
            String json = fileContentJson("encoded_content\\nwith_newlines", "base64");

            exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(json)
                    .build());

            FileContentDto result = githubApiClient.getFileContent("facebook", "react", "README.md", "main");

            assertNotNull(result);
            assertEquals("encoded_content\nwith_newlines", result.getContent());
            assertEquals("base64", result.getEncoding());
            assertEquals(1024L, result.getSize());
        }

        @Test
        @DisplayName("E. Throws GitHubApiException (400) when GitHub returns a directory (JSON array)")
        void directoryResponse() {
            String directoryJsonArray = """
                    [
                        {
                            "name": "src",
                            "path": "src",
                            "type": "dir"
                        }
                    ]
                    """;

            exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .body(directoryJsonArray)
                    .build());

            GitHubApiException ex = assertThrows(GitHubApiException.class, () ->
                    githubApiClient.getFileContent("facebook", "react", "src", "main"));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertEquals("Requested path is a directory, not a file.", ex.getMessage());
        }

        @Test
        @DisplayName("F. Throws GitHubApiException (401) on invalid or expired token")
        void unauthorized() {
            exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED).build());

            GitHubApiException ex = assertThrows(GitHubApiException.class, () ->
                    githubApiClient.getFileContent("facebook", "react", "README.md", "main"));

            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
            assertEquals("Invalid or expired GitHub token.", ex.getMessage());
            assertFalse(ex.getMessage().contains("test-token-123"));
        }

        @Test
        @DisplayName("G. Throws GitHubApiException (429) on 403 forbidden / rate limit")
        void forbidden() {
            exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.FORBIDDEN).build());

            GitHubApiException ex = assertThrows(GitHubApiException.class, () ->
                    githubApiClient.getFileContent("facebook", "react", "README.md", "main"));

            assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
            assertEquals("GitHub API rate limit exceeded or access forbidden.", ex.getMessage());
        }

        @Test
        @DisplayName("H. Throws GitHubApiException (404) when file not found")
        void notFound() {
            exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());

            GitHubApiException ex = assertThrows(GitHubApiException.class, () ->
                    githubApiClient.getFileContent("facebook", "react", "nonexistent.txt", "main"));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
            assertEquals("GitHub repository or file not found.", ex.getMessage());
        }

        @Test
        @DisplayName("I. Throws GitHubApiException (429) on explicit rate limit")
        void tooManyRequests() {
            exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.TOO_MANY_REQUESTS).build());

            GitHubApiException ex = assertThrows(GitHubApiException.class, () ->
                    githubApiClient.getFileContent("facebook", "react", "README.md", "main"));

            assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        }

        @Test
        @DisplayName("J. Throws GitHubApiException (502) on 5xx server errors")
        void serverError() {
            exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build());

            GitHubApiException ex = assertThrows(GitHubApiException.class, () ->
                    githubApiClient.getFileContent("facebook", "react", "README.md", "main"));

            assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
            assertEquals("GitHub API is unavailable.", ex.getMessage());
        }

        @Test
        @DisplayName("K. Throws GitHubApiException (502) on network failure/timeout")
        void networkFailure() {
            exchangeFunction = request -> Mono.error(new RuntimeException("Connection timed out"));

            GitHubApiException ex = assertThrows(GitHubApiException.class, () ->
                    githubApiClient.getFileContent("facebook", "react", "README.md", "main"));

            assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
            assertEquals("Failed to communicate with GitHub API.", ex.getMessage());
        }
    }
}
