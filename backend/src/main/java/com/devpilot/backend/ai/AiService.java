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
    /**
     * Sends a generic prompt to the underlying AI provider.
     * This method serves as a foundation for future specific use cases.
     *
     * @param prompt The input text prompt to send to the AI model.
     * @return The AI model's response.
     */
    String generateResponse(String prompt);

    /**
     * Retrieves a file from the connected GitHub repository and requests the AI provider
     * to explain it.
     *
     * @param projectId The ID of the project.
     * @param path The path of the file in the repository.
     * @return The AI-generated explanation.
     */
    String explainCode(Long projectId, String path);

    /**
     * Retrieves a file from the connected GitHub repository and requests the AI provider
     * to detect bugs and defects in it.
     *
     * @param projectId The ID of the project.
     * @param path The path of the file in the repository.
     * @return A structured bug-analysis response DTO.
     */
    com.devpilot.backend.dto.AiBugAnalysisResponseDto analyzeBugs(Long projectId, String path);

    /**
     * Retrieves a file from the connected GitHub repository and requests the AI provider
     * to identify meaningful opportunities to improve the code.
     *
     * @param projectId The ID of the project.
     * @param path The path of the file in the repository.
     * @return A structured code-improvement response DTO.
     */
    com.devpilot.backend.dto.AiImprovementResponseDto suggestImprovements(Long projectId, String path);

    /**
     * Retrieves a file from the connected GitHub repository and requests the AI provider
     * to identify meaningful unit tests that should be written for the code.
     *
     * @param projectId The ID of the project.
     * @param path The path of the file in the repository.
     * @return A structured test-suggestion response DTO.
     */
    com.devpilot.backend.dto.AiTestSuggestionResponseDto suggestTests(Long projectId, String path);
}
