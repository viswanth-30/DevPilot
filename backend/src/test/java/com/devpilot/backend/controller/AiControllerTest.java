package com.devpilot.backend.controller;

import com.devpilot.backend.ai.AiService;
import com.devpilot.backend.dto.AiBugAnalysisResponseDto;
import com.devpilot.backend.dto.AiBugDto;
import com.devpilot.backend.dto.AiExplainRequestDto;
import com.devpilot.backend.dto.BugSeverity;
import com.devpilot.backend.exception.AiException;
import com.devpilot.backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
@DisplayName("AiController")
public class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiService aiService;

    // =========================================================================
    // POST /api/ai/explain/{projectId}
    // =========================================================================

    @Nested
    @DisplayName("POST /api/ai/explain/{projectId}")
    class ExplainEndpoint {

        @Test
        void successReturns200() throws Exception {
            when(aiService.explainCode(1L, "src/Main.java"))
                    .thenReturn("This is a mock explanation.");

            mockMvc.perform(post("/api/ai/explain/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"path\":\"src/Main.java\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.projectId").value(1))
                    .andExpect(jsonPath("$.path").value("src/Main.java"))
                    .andExpect(jsonPath("$.explanation").value("This is a mock explanation."));
        }

        @Test
        void missingPathReturns400() throws Exception {
            mockMvc.perform(post("/api/ai/explain/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"path\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation Failed"));
        }

        @Test
        void projectNotFoundReturns404() throws Exception {
            when(aiService.explainCode(99L, "src/Main.java"))
                    .thenThrow(new ResourceNotFoundException("Project not found with id: 99"));

            mockMvc.perform(post("/api/ai/explain/99")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"path\":\"src/Main.java\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Not Found"));
        }

        @Test
        void aiExceptionReturns429() throws Exception {
            when(aiService.explainCode(1L, "src/Main.java"))
                    .thenThrow(new AiException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded"));

            mockMvc.perform(post("/api/ai/explain/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"path\":\"src/Main.java\"}"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.message").value("Rate limit exceeded"));
        }
    }

    // =========================================================================
    // POST /api/ai/analyze/{projectId}
    // =========================================================================

    @Nested
    @DisplayName("POST /api/ai/analyze/{projectId}")
    class AnalyzeEndpoint {

        private AiBugAnalysisResponseDto sampleResponse() {
            AiBugDto bug = new AiBugDto(
                    BugSeverity.HIGH,
                    "Null dereference",
                    "foo() may return null.",
                    "42",
                    "Add a null check."
            );
            return new AiBugAnalysisResponseDto(
                    2L,
                    "src/Service.java",
                    "One potential null dereference found.",
                    List.of(bug)
            );
        }

        @Test
        void successReturns200WithStructuredResponse() throws Exception {
            when(aiService.analyzeBugs(2L, "src/Service.java")).thenReturn(sampleResponse());

            mockMvc.perform(post("/api/ai/analyze/2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"path\":\"src/Service.java\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.projectId").value(2))
                    .andExpect(jsonPath("$.path").value("src/Service.java"))
                    .andExpect(jsonPath("$.summary").value("One potential null dereference found."))
                    .andExpect(jsonPath("$.bugs").isArray())
                    .andExpect(jsonPath("$.bugs[0].severity").value("HIGH"))
                    .andExpect(jsonPath("$.bugs[0].title").value("Null dereference"))
                    .andExpect(jsonPath("$.bugs[0].lineReference").value("42"));
        }

        @Test
        void missingPathReturns400() throws Exception {
            mockMvc.perform(post("/api/ai/analyze/2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"path\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation Failed"));
        }

        @Test
        void projectNotFoundReturns404() throws Exception {
            when(aiService.analyzeBugs(99L, "src/Main.java"))
                    .thenThrow(new ResourceNotFoundException("Project not found with id: 99"));

            mockMvc.perform(post("/api/ai/analyze/99")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"path\":\"src/Main.java\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Not Found"));
        }

        @Test
        void aiExceptionReturns500() throws Exception {
            when(aiService.analyzeBugs(2L, "src/Main.java"))
                    .thenThrow(new AiException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "AI provider returned a response that could not be parsed as structured bug analysis."));

            mockMvc.perform(post("/api/ai/analyze/2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"path\":\"src/Main.java\"}"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value(
                            "AI provider returned a response that could not be parsed as structured bug analysis."));
        }

        @Test
        void directoryPathReturns400() throws Exception {
            when(aiService.analyzeBugs(2L, "src"))
                    .thenThrow(new AiException(HttpStatus.BAD_REQUEST,
                            "Requested path is a directory. Please specify a file."));

            mockMvc.perform(post("/api/ai/analyze/2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"path\":\"src\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Requested path is a directory. Please specify a file."));
        }

        @Test
        void rateLimitReturns429() throws Exception {
            when(aiService.analyzeBugs(2L, "src/Main.java"))
                    .thenThrow(new AiException(HttpStatus.TOO_MANY_REQUESTS, "AI provider rate limit exceeded. Please try again later."));

            mockMvc.perform(post("/api/ai/analyze/2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"path\":\"src/Main.java\"}"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.message").value("AI provider rate limit exceeded. Please try again later."));
        }
    }

    // =========================================================================
    // POST /api/ai/improve/{projectId}
    // =========================================================================

    @Nested
    @DisplayName("POST /api/ai/improve/{projectId}")
    class ImproveEndpoint {

        private com.devpilot.backend.dto.AiImprovementResponseDto sampleResponse() {
            com.devpilot.backend.dto.AiImprovementDto improvement = new com.devpilot.backend.dto.AiImprovementDto(
                    com.devpilot.backend.dto.ImprovementCategory.MAINTAINABILITY,
                    com.devpilot.backend.dto.ImprovementPriority.MEDIUM,
                    "Extract logic",
                    "Extract repeated logic.",
                    "42-55",
                    "Create a method."
            );
            return new com.devpilot.backend.dto.AiImprovementResponseDto(
                    2L,
                    "src/Service.java",
                    "Found one improvement.",
                    List.of(improvement)
            );
        }

        @Test
        void successReturns200WithStructuredResponse() throws Exception {
            when(aiService.suggestImprovements(2L, "src/Service.java")).thenReturn(sampleResponse());

            mockMvc.perform(post("/api/ai/improve/2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"path\":\"src/Service.java\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.projectId").value(2))
                    .andExpect(jsonPath("$.path").value("src/Service.java"))
                    .andExpect(jsonPath("$.summary").value("Found one improvement."))
                    .andExpect(jsonPath("$.suggestions").isArray())
                    .andExpect(jsonPath("$.suggestions[0].category").value("MAINTAINABILITY"))
                    .andExpect(jsonPath("$.suggestions[0].priority").value("MEDIUM"))
                    .andExpect(jsonPath("$.suggestions[0].title").value("Extract logic"))
                    .andExpect(jsonPath("$.suggestions[0].lineReference").value("42-55"));
        }

        @Test
        void missingPathReturns400() throws Exception {
            mockMvc.perform(post("/api/ai/improve/2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"path\":\"\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation Failed"));
        }

        @Test
        void projectNotFoundReturns404() throws Exception {
            when(aiService.suggestImprovements(99L, "src/Main.java"))
                    .thenThrow(new ResourceNotFoundException("Project not found with id: 99"));

            mockMvc.perform(post("/api/ai/improve/99")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"path\":\"src/Main.java\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Not Found"));
        }

        @Test
        void aiExceptionReturns500() throws Exception {
            when(aiService.suggestImprovements(2L, "src/Main.java"))
                    .thenThrow(new AiException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "AI provider returned a response that could not be parsed as structured code improvement suggestions."));

            mockMvc.perform(post("/api/ai/improve/2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"path\":\"src/Main.java\"}"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.message").value(
                            "AI provider returned a response that could not be parsed as structured code improvement suggestions."));
        }

        @Test
        void rateLimitReturns429() throws Exception {
            when(aiService.suggestImprovements(2L, "src/Main.java"))
                    .thenThrow(new AiException(HttpStatus.TOO_MANY_REQUESTS, "AI provider rate limit exceeded. Please try again later."));

            mockMvc.perform(post("/api/ai/improve/2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"path\":\"src/Main.java\"}"))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.message").value("AI provider rate limit exceeded. Please try again later."));
        }
    }
}
