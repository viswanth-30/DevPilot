package com.devpilot.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents the file tree of a GitHub repository, as returned by the
 * GitHub Git Trees API: GET /repos/{owner}/{repo}/git/trees/{branch}?recursive=1
 *
 * Two-level structure:
 *   FileTreeDto            — top-level wrapper (truncated flag + list of entries)
 *   FileTreeDto.TreeEntryDto — a single item in the tree (file or directory)
 *
 * The {@code truncated} field is preserved and NOT hidden — if GitHub returns
 * true, the tree is incomplete and callers must be aware of this.
 */
public class FileTreeDto {

    /**
     * Whether GitHub's response was truncated because the tree is too large.
     * If true, the {@code tree} list is incomplete.
     */
    private boolean truncated;

    /**
     * The list of entries in the repository tree.
     * Each entry represents either a blob (file) or tree (directory).
     */
    private List<TreeEntryDto> tree;

    // ─── Constructors ─────────────────────────────────────────────────────────

    public FileTreeDto() {
    }

    public FileTreeDto(boolean truncated, List<TreeEntryDto> tree) {
        this.truncated = truncated;
        this.tree = tree;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public List<TreeEntryDto> getTree() {
        return tree;
    }

    public void setTree(List<TreeEntryDto> tree) {
        this.tree = tree;
    }

    // ─── Nested DTO ───────────────────────────────────────────────────────────

    /**
     * Represents a single entry in the repository's Git tree.
     *
     * GitHub's API uses numeric file-mode strings (e.g. "100644" for a regular
     * file, "040000" for a directory, "160000" for a submodule).
     *
     * The {@code size} field is nullable because tree-type entries (directories
     * and submodules) do not have a size in the GitHub API response.
     */
    public static class TreeEntryDto {

        /**
         * The full path of this entry relative to the repository root.
         * e.g. "src/main/java/com/example/App.java"
         */
        private String path;

        /**
         * The Git file mode string.
         * e.g. "100644" (regular file), "040000" (directory), "160000" (submodule)
         */
        private String mode;

        /**
         * The entry type: "blob" (file), "tree" (directory), or "commit" (submodule).
         */
        private String type;

        /**
         * The SHA-1 hash of this tree object.
         */
        private String sha;

        /**
         * The file size in bytes. Null for tree-type entries (directories, submodules).
         */
        @JsonProperty("size")
        private Long size;

        /**
         * The GitHub API URL for this specific tree entry.
         */
        private String url;

        // ─── Constructors ──────────────────────────────────────────────────

        public TreeEntryDto() {
        }

        // ─── Getters & Setters ─────────────────────────────────────────────

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getSha() {
            return sha;
        }

        public void setSha(String sha) {
            this.sha = sha;
        }

        public Long getSize() {
            return size;
        }

        public void setSize(Long size) {
            this.size = size;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
