package com.devpilot.backend.github;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for GitHubUrlParser.
 *
 * No Spring context is loaded — this is a pure Java test.
 * Uses JUnit 5 (Jupiter), which is included via spring-boot-starter-webmvc-test.
 */
@DisplayName("GitHubUrlParser")
class GitHubUrlParserTest {

    // ─── Valid URL tests ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Valid URLs")
    class ValidUrls {

        @Test
        @DisplayName("parses standard https URL correctly")
        void standardHttpsUrl() {
            var result = GitHubUrlParser.parse("https://github.com/facebook/react");
            assertEquals("facebook", result.getOwner());
            assertEquals("react", result.getRepoName());
        }

        @Test
        @DisplayName("parses URL with trailing slash")
        void trailingSlash() {
            var result = GitHubUrlParser.parse("https://github.com/facebook/react/");
            assertEquals("facebook", result.getOwner());
            assertEquals("react", result.getRepoName());
        }

        @Test
        @DisplayName("strips .git suffix from repo name")
        void gitSuffix() {
            var result = GitHubUrlParser.parse("https://github.com/facebook/react.git");
            assertEquals("facebook", result.getOwner());
            assertEquals("react", result.getRepoName());
        }

        @Test
        @DisplayName("accepts http scheme (not just https)")
        void httpScheme() {
            var result = GitHubUrlParser.parse("http://github.com/torvalds/linux");
            assertEquals("torvalds", result.getOwner());
            assertEquals("linux", result.getRepoName());
        }

        @Test
        @DisplayName("accepts owner/repo names with hyphens and digits")
        void namesWithHyphensAndDigits() {
            var result = GitHubUrlParser.parse("https://github.com/spring-projects/spring-boot");
            assertEquals("spring-projects", result.getOwner());
            assertEquals("spring-boot", result.getRepoName());
        }

        @Test
        @DisplayName("accepts owner/repo names with dots")
        void namesWithDots() {
            var result = GitHubUrlParser.parse("https://github.com/some.org/some.repo");
            assertEquals("some.org", result.getOwner());
            assertEquals("some.repo", result.getRepoName());
        }

        @Test
        @DisplayName("accepts owner/repo names with underscores")
        void namesWithUnderscores() {
            var result = GitHubUrlParser.parse("https://github.com/some_org/some_repo");
            assertEquals("some_org", result.getOwner());
            assertEquals("some_repo", result.getRepoName());
        }

        @Test
        @DisplayName("toString() produces canonical URL")
        void toStringCanonical() {
            var result = GitHubUrlParser.parse("https://github.com/facebook/react.git");
            assertEquals("https://github.com/facebook/react", result.toString());
        }
    }

    // ─── Invalid URL tests ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Invalid URLs — should throw IllegalArgumentException")
    class InvalidUrls {

        @Test
        @DisplayName("rejects null input")
        void nullInput() {
            assertThrows(IllegalArgumentException.class,
                    () -> GitHubUrlParser.parse(null));
        }

        @Test
        @DisplayName("rejects blank string")
        void blankString() {
            assertThrows(IllegalArgumentException.class,
                    () -> GitHubUrlParser.parse("   "));
        }

        @Test
        @DisplayName("rejects empty string")
        void emptyString() {
            assertThrows(IllegalArgumentException.class,
                    () -> GitHubUrlParser.parse(""));
        }

        @ParameterizedTest(name = "rejects non-GitHub host: {0}")
        @ValueSource(strings = {
                "https://gitlab.com/owner/repo",
                "https://bitbucket.org/owner/repo",
                "https://github.io/owner/repo",
                "https://notgithub.com/owner/repo"
        })
        @DisplayName("rejects non-GitHub hosts")
        void nonGitHubHost(String url) {
            assertThrows(IllegalArgumentException.class,
                    () -> GitHubUrlParser.parse(url));
        }

        @ParameterizedTest(name = "rejects invalid scheme: {0}")
        @ValueSource(strings = {
                "ftp://github.com/owner/repo",
                "ssh://github.com/owner/repo",
                "git://github.com/owner/repo"
        })
        @DisplayName("rejects non-http/https schemes")
        void invalidScheme(String url) {
            assertThrows(IllegalArgumentException.class,
                    () -> GitHubUrlParser.parse(url));
        }

        @Test
        @DisplayName("rejects URL with no scheme (missing protocol)")
        void missingScheme() {
            assertThrows(IllegalArgumentException.class,
                    () -> GitHubUrlParser.parse("github.com/owner/repo"));
        }

        @Test
        @DisplayName("rejects URL with only owner, no repo")
        void missingRepo() {
            assertThrows(IllegalArgumentException.class,
                    () -> GitHubUrlParser.parse("https://github.com/owner"));
        }

        @Test
        @DisplayName("rejects URL with only owner and trailing slash, no repo")
        void ownerOnlyWithSlash() {
            assertThrows(IllegalArgumentException.class,
                    () -> GitHubUrlParser.parse("https://github.com/owner/"));
        }

        @Test
        @DisplayName("rejects URL with no path at all")
        void noPath() {
            assertThrows(IllegalArgumentException.class,
                    () -> GitHubUrlParser.parse("https://github.com"));
        }

        @ParameterizedTest(name = "rejects subpath URL: {0}")
        @ValueSource(strings = {
                "https://github.com/owner/repo/issues",
                "https://github.com/owner/repo/tree/main",
                "https://github.com/owner/repo/blob/main/README.md",
                "https://github.com/owner/repo/pulls",
                "https://github.com/owner/repo/actions"
        })
        @DisplayName("rejects repository subpath URLs")
        void subpathUrls(String url) {
            assertThrows(IllegalArgumentException.class,
                    () -> GitHubUrlParser.parse(url));
        }

        @Test
        @DisplayName("rejects URL with query string")
        void withQueryString() {
            assertThrows(IllegalArgumentException.class,
                    () -> GitHubUrlParser.parse("https://github.com/owner/repo?tab=readme"));
        }

        @Test
        @DisplayName("rejects URL with fragment")
        void withFragment() {
            assertThrows(IllegalArgumentException.class,
                    () -> GitHubUrlParser.parse("https://github.com/owner/repo#readme"));
        }

        @Test
        @DisplayName("rejects .git suffix leaving empty repo name")
        void gitSuffixOnly() {
            // ".git" as repo name → after stripping → blank
            assertThrows(IllegalArgumentException.class,
                    () -> GitHubUrlParser.parse("https://github.com/owner/.git"));
        }
    }
}
