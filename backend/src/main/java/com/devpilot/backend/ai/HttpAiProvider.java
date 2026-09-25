package com.devpilot.backend.ai;

import com.devpilot.backend.exception.AiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.util.List;
import java.util.Map;

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
        this.model = properties.getModel() != null && !properties.getModel().isBlank() ? properties.getModel() : "gpt-4o";
    }

    @Override
    public String generateResponse(String prompt) {
        AiRequest requestBody = new AiRequest(model, List.of(new Message("user", prompt)));

        try {
            AiResponse response = webClient.post()
                    // Assuming OpenAI-compatible endpoint like /chat/completions
                    // However, we don't append to baseUrl because baseUrl might already contain the full path depending on the config.
                    // A safer approach is appending nothing if the baseUrl is fully qualified, but standard OpenAI uses /chat/completions
                    // For maximum compatibility with just a base URL like "https://api.openai.com/v1", we add /chat/completions
                    .uri(uriBuilder -> uriBuilder.path("/chat/completions").build())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(AiResponse.class)
                    .block();

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                throw new AiException(HttpStatus.INTERNAL_SERVER_ERROR, "Received malformed or empty response from AI provider.");
            }

            return response.choices().get(0).message().content();

        } catch (WebClientResponseException e) {
            // Handle HTTP errors returned by the AI provider (4xx, 5xx)
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;

            String errorMessage = switch (status) {
                case UNAUTHORIZED -> "AI provider authentication failed. Please check the AI API key.";
                case TOO_MANY_REQUESTS -> "AI provider rate limit exceeded. Please try again later.";
                default -> "AI provider returned an error: " + status.getReasonPhrase();
            };

            throw new AiException(status, errorMessage);

        } catch (WebClientRequestException e) {
            // Handle network errors (timeout, DNS, connection refused)
            throw new AiException(HttpStatus.BAD_GATEWAY, "Network error while communicating with AI provider.");
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            // Catch-all for JSON parsing issues or other unexpected errors
            throw new AiException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred while communicating with the AI provider.");
        }
    }

    // --- Private inner classes for isolated JSON schema ---

    private record AiRequest(String model, List<Message> messages) {}
    private record Message(String role, String content) {}

    private record AiResponse(List<Choice> choices) {}
    private record Choice(Message message) {}
}
