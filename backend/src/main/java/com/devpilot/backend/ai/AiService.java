package com.devpilot.backend.ai;

/**
 * High-level AI service used by controllers or other application services.
 * Independent of any specific AI provider implementation.
 * Provides the foundation for future capabilities like code explanation,
 * bug detection, and test generation.
 */
public interface AiService {

    /**
     * Sends a generic prompt to the underlying AI provider.
     * This method serves as a foundation for future specific use cases.
     * 
     * @param prompt The input text prompt to send to the AI model.
     * @return The AI model's response.
     */
    String generateResponse(String prompt);
}
