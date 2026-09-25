package com.devpilot.backend.ai;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class HttpAiProvider implements AiProvider {

    private final WebClient webClient;
    private final String model;

    public HttpAiProvider(WebClient.Builder webClientBuilder, AiProperties properties) {
        String baseUrl = properties.getBaseUrl() != null ? properties.getBaseUrl() : "https://api.openai.com/v1";
        String apiKey = properties.getKey() != null ? properties.getKey() : "";
        
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        this.model = properties.getModel();
    }

    @Override
    public String generateResponse(String prompt) {
        // Implementation for future milestones.
        // For this milestone, we do not make a real API call.
        throw new UnsupportedOperationException("AI integration is not fully implemented yet.");
    }
}
