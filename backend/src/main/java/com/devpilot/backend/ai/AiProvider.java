package com.devpilot.backend.ai;

/**
 * Abstraction for an external AI provider.
 * Allows the application to switch providers (e.g. OpenAI, Anthropic, Gemini) 
 * without modifying the core service layer.
 */
public interface AiProvider {

    /**
     * Sends a prompt to the AI provider and returns the raw response string.
     * 
     * @param prompt The input text prompt to send to the AI model.
     * @return The AI model's response.
     */
    String generateResponse(String prompt);
}
