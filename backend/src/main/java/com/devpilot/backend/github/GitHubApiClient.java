package com.devpilot.backend.github;

import com.devpilot.backend.dto.FileContentDto;
import com.devpilot.backend.dto.FileTreeDto;
import com.devpilot.backend.dto.RepoMetadataDto;
import com.devpilot.backend.exception.GitHubApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Client for interacting with the GitHub REST API.
 * Uses Spring WebFlux WebClient synchronously.
 *
 * Does NOT interact with the database or business logic layers.
 * A single shared WebClient instance is used for all methods — the base URL
 * and authentication headers are set once in the constructor.
 */
@Component
public class GitHubApiClient {

    private final WebClient webClient;

    public GitHubApiClient(
            WebClient.Builder webClientBuilder,
            @Value("${github.api.base-url}") String baseUrl,
            @Value("${github.api.token}") String token) {

        // Build the WebClient with default headers — shared by all methods.
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github.v3+json")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    /**
     * Fetches basic repository metadata from GitHub.
     * GET /repos/{owner}/{repo}
     *
     * @param owner The repository owner (e.g., "spring-projects")
     * @param repo  The repository name (e.g., "spring-boot")
     * @return RepoMetadataDto containing key details about the repository.
     * @throws GitHubApiException on any HTTP or network error.
     */
    public RepoMetadataDto getRepositoryMetadata(String owner, String repo) {
        try {
            return webClient.get()
                    .uri("/repos/{owner}/{repo}", owner, repo)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), response -> {
                        HttpStatus status = (HttpStatus) response.statusCode();
                        if (status == HttpStatus.UNAUTHORIZED) {
                            return Mono.error(new GitHubApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired GitHub token."));
                        } else if (status == HttpStatus.NOT_FOUND) {
                            return Mono.error(new GitHubApiException(HttpStatus.NOT_FOUND, "GitHub repository not found."));
                        } else if (status == HttpStatus.FORBIDDEN || status == HttpStatus.TOO_MANY_REQUESTS) {
                            return Mono.error(new GitHubApiException(HttpStatus.TOO_MANY_REQUESTS, "GitHub API rate limit exceeded or access forbidden."));
                        }
                        return Mono.error(new GitHubApiException(status, "GitHub API client error."));
                    })
                    .onStatus(status -> status.is5xxServerError(), response ->
                        Mono.error(new GitHubApiException(HttpStatus.BAD_GATEWAY, "GitHub API is unavailable."))
                    )
                    .bodyToMono(RepoMetadataDto.class)
                    .block();

        } catch (WebClientResponseException e) {
            throw new GitHubApiException(HttpStatus.BAD_GATEWAY, "Unexpected response from GitHub API.");
        } catch (GitHubApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GitHubApiException(HttpStatus.BAD_GATEWAY, "Failed to communicate with GitHub API.");
        }
    }

    /**
     * Fetches the complete recursive file tree of a GitHub repository.
     * GET /repos/{owner}/{repo}/git/trees/{branch}?recursive=1
     *
     * The GitHub Git Trees API returns all blobs (files) and trees (directories)
     * reachable from the given branch tip in a single flat list.
     *
     * If the repository is very large, GitHub may set {@code truncated=true} on
     * the response, indicating the list is incomplete. This is preserved in the
     * returned DTO — it is NOT silently ignored.
     *
     * @param owner  The repository owner (e.g., "facebook")
     * @param repo   The repository name (e.g., "react")
     * @param branch The branch name (e.g., "main")
     * @return FileTreeDto containing the full tree and whether it was truncated.
     * @throws GitHubApiException on any HTTP or network error.
     */
    public FileTreeDto getRepositoryTree(String owner, String repo, String branch) {
        try {
            return webClient.get()
                    .uri("/repos/{owner}/{repo}/git/trees/{branch}?recursive=1", owner, repo, branch)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), response -> {
                        HttpStatus status = (HttpStatus) response.statusCode();
                        if (status == HttpStatus.UNAUTHORIZED) {
                            return Mono.error(new GitHubApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired GitHub token."));
                        } else if (status == HttpStatus.NOT_FOUND) {
                            return Mono.error(new GitHubApiException(HttpStatus.NOT_FOUND, "GitHub repository tree not found."));
                        } else if (status == HttpStatus.FORBIDDEN || status == HttpStatus.TOO_MANY_REQUESTS) {
                            return Mono.error(new GitHubApiException(HttpStatus.TOO_MANY_REQUESTS, "GitHub API rate limit exceeded or access forbidden."));
                        }
                        return Mono.error(new GitHubApiException(status, "GitHub API client error."));
                    })
                    .onStatus(status -> status.is5xxServerError(), response ->
                        Mono.error(new GitHubApiException(HttpStatus.BAD_GATEWAY, "GitHub API is unavailable."))
                    )
                    .bodyToMono(FileTreeDto.class)
                    .block();

        } catch (WebClientResponseException e) {
            throw new GitHubApiException(HttpStatus.BAD_GATEWAY, "Unexpected response from GitHub API.");
        } catch (GitHubApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GitHubApiException(HttpStatus.BAD_GATEWAY, "Failed to communicate with GitHub API.");
        }
    }
    /**
     * Fetches the contents of a specific file from a GitHub repository.
     * GET /repos/{owner}/{repo}/contents/{path}?ref={branch}
     *
     * @param owner  The repository owner (e.g., "facebook")
     * @param repo   The repository name (e.g., "react")
     * @param path   The file path (e.g., "src/index.js")
     * @param branch The branch name (e.g., "main")
     * @return FileContentDto containing the file metadata and encoded content.
     * @throws GitHubApiException on any HTTP or network error, or if the path is a directory.
     */
    public FileContentDto getFileContent(String owner, String repo, String path, String branch) {
        try {
            return webClient.get()
                    .uri("/repos/{owner}/{repo}/contents/{path}?ref={branch}", owner, repo, path, branch)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), response -> {
                        HttpStatus status = (HttpStatus) response.statusCode();
                        if (status == HttpStatus.UNAUTHORIZED) {
                            return Mono.error(new GitHubApiException(HttpStatus.UNAUTHORIZED, "Invalid or expired GitHub token."));
                        } else if (status == HttpStatus.NOT_FOUND) {
                            return Mono.error(new GitHubApiException(HttpStatus.NOT_FOUND, "GitHub repository or file not found."));
                        } else if (status == HttpStatus.FORBIDDEN || status == HttpStatus.TOO_MANY_REQUESTS) {
                            return Mono.error(new GitHubApiException(HttpStatus.TOO_MANY_REQUESTS, "GitHub API rate limit exceeded or access forbidden."));
                        }
                        return Mono.error(new GitHubApiException(status, "GitHub API client error."));
                    })
                    .onStatus(status -> status.is5xxServerError(), response ->
                        Mono.error(new GitHubApiException(HttpStatus.BAD_GATEWAY, "GitHub API is unavailable."))
                    )
                    .bodyToMono(FileContentDto.class)
                    .onErrorMap(org.springframework.core.codec.DecodingException.class, ex ->
                            new GitHubApiException(HttpStatus.BAD_REQUEST, "Requested path is a directory, not a file.")
                    )
                    .block();

        } catch (WebClientResponseException e) {
            throw new GitHubApiException(HttpStatus.BAD_GATEWAY, "Unexpected response from GitHub API.");
        } catch (GitHubApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GitHubApiException(HttpStatus.BAD_GATEWAY, "Failed to communicate with GitHub API.");
        }
    }
}
