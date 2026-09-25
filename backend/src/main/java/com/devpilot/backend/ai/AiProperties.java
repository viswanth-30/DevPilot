package com.devpilot.backend.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the AI integration.
 * Prefix is "ai.api", so these map to "ai.api.base-url", "ai.api.key", "ai.api.model"
 * in application.properties or environment variables.
 */
@Configuration
@ConfigurationProperties(prefix = "ai.api")
public class AiProperties {

    private String baseUrl;
    private String key;
    private String model;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
