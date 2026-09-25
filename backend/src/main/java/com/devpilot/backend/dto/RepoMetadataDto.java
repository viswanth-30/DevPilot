package com.devpilot.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the repository metadata returned by the GitHub REST API.
 * Maps only the fields we care about for the current and upcoming milestones.
 */
public class RepoMetadataDto {

    private String name;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("default_branch")
    private String defaultBranch;

    // Using wrapper type Boolean in case it's missing (though GitHub always returns it)
    @JsonProperty("private")
    private Boolean isPrivate;

    // We can map nested JSON structures using static inner classes or custom deserializers.
    // GitHub returns 'owner': { 'login': '...' }
    private Owner owner;

    public RepoMetadataDto() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public Boolean getPrivate() {
        return isPrivate;
    }

    public void setPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    /**
     * Convenience method to extract owner login directly.
     */
    public String getOwnerLogin() {
        return owner != null ? owner.getLogin() : null;
    }

    public static class Owner {
        private String login;

        public Owner() {
        }

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }
    }
}
