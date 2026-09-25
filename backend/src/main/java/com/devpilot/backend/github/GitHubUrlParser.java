package com.devpilot.backend.github;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * Pure utility class for parsing and validating GitHub repository URLs.
 *
 * This class has no Spring annotations, no dependencies, and no state.
 * It can be used and unit-tested without a Spring context.
 *
 * Accepted URL formats:
 *   https://github.com/owner/repo
 *   https://github.com/owner/repo/
 *   https://github.com/owner/repo.git
 *   http://github.com/owner/repo        (normalised internally)
 *
 * Rejected:
 *   https://gitlab.com/owner/repo       (wrong host)
 *   github.com/owner/repo               (missing scheme)
 *   https://github.com/owner            (missing repo name)
 *   https://github.com/owner/repo/issues (subpath — not a repo root)
 *   ftp://github.com/owner/repo         (invalid scheme)
 *   any malformed or null URL
 */
public final class GitHubUrlParser {

    // GitHub allows letters, digits, hyphens, underscores and dots in names.
    private static final Pattern VALID_NAME = Pattern.compile("^[a-zA-Z0-9._-]+$");

    // Private constructor — this class is never instantiated.
    private GitHubUrlParser() {
    }

    // ─── Public result type ───────────────────────────────────────────────────

    /**
     * Immutable value object returned by a successful parse.
     */
    public static final class ParsedGitHubUrl {

        private final String owner;
        private final String repoName;

        private ParsedGitHubUrl(String owner, String repoName) {
            this.owner = owner;
            this.repoName = repoName;
        }

        public String getOwner() {
            return owner;
        }

        public String getRepoName() {
            return repoName;
        }

        @Override
        public String toString() {
            return "https://github.com/" + owner + "/" + repoName;
        }
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Parses a GitHub repository URL into owner and repository name.
     *
     * @param url the URL string to parse (may be null)
     * @return ParsedGitHubUrl containing owner and repoName
     * @throws IllegalArgumentException if the URL is null, malformed, not a
     *         GitHub repository root, or contains a subpath
     */
    public static ParsedGitHubUrl parse(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException(
                    "GitHub URL must not be null or blank.");
        }

        String trimmed = url.strip();

        // ── Step 1: parse as a URI to extract components cleanly ─────────────
        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(
                    "Invalid GitHub URL — could not be parsed as a URI: " + trimmed);
        }

        // ── Step 2: scheme must be http or https ──────────────────────────────
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equals("https") && !scheme.equals("http"))) {
            throw new IllegalArgumentException(
                    "Invalid GitHub URL — scheme must be 'https' or 'http', got: "
                            + (scheme == null ? "(none)" : scheme));
        }

        // ── Step 3: host must be exactly github.com ───────────────────────────
        String host = uri.getHost();
        if (host == null || !host.equalsIgnoreCase("github.com")) {
            throw new IllegalArgumentException(
                    "Invalid GitHub URL — host must be 'github.com', got: "
                            + (host == null ? "(none)" : host));
        }

        // ── Step 4: no query string, no fragment, no user-info ───────────────
        if (uri.getQuery() != null) {
            throw new IllegalArgumentException(
                    "Invalid GitHub URL — URL must not contain a query string: " + trimmed);
        }
        if (uri.getFragment() != null) {
            throw new IllegalArgumentException(
                    "Invalid GitHub URL — URL must not contain a fragment: " + trimmed);
        }

        // ── Step 5: split the path into segments ─────────────────────────────
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid GitHub URL — URL has no path (missing owner/repo).");
        }

        // Remove leading slash, trailing slash, and split.
        // "/owner/repo/" → ["owner", "repo"]
        String[] segments = path.replaceAll("^/|/$", "").split("/");

        // ── Step 6: exactly 2 path segments (owner and repo) ─────────────────
        if (segments.length < 2 || segments[0].isBlank() || segments[1].isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid GitHub URL — must contain exactly owner and repository name, e.g. "
                            + "https://github.com/owner/repo. Got path: " + path);
        }
        if (segments.length > 2) {
            throw new IllegalArgumentException(
                    "Invalid GitHub URL — URL must point to a repository root, not a subpath "
                            + "(e.g. /issues, /tree, /blob). Got path: " + path);
        }

        // ── Step 7: strip .git suffix from repo name if present ──────────────
        String owner = segments[0];
        String repoName = segments[1];
        if (repoName.endsWith(".git")) {
            repoName = repoName.substring(0, repoName.length() - 4);
            if (repoName.isBlank()) {
                throw new IllegalArgumentException(
                        "Invalid GitHub URL — repository name is empty after removing .git suffix.");
            }
        }

        // ── Step 8: validate owner and repo name characters ──────────────────
        if (!VALID_NAME.matcher(owner).matches()) {
            throw new IllegalArgumentException(
                    "Invalid GitHub URL — owner name contains invalid characters: " + owner);
        }
        if (!VALID_NAME.matcher(repoName).matches()) {
            throw new IllegalArgumentException(
                    "Invalid GitHub URL — repository name contains invalid characters: " + repoName);
        }

        return new ParsedGitHubUrl(owner, repoName);
    }
}
