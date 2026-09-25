package com.devpilot.backend.controller;

import com.devpilot.backend.ai.AiService;
import com.devpilot.backend.dto.AiExplainRequestDto;
import com.devpilot.backend.exception.AiException;
import com.devpilot.backend.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
public class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiService aiService;

    @Test
    void testExplainCodeSuccess() throws Exception {
        Long projectId = 1L;
        AiExplainRequestDto request = new AiExplainRequestDto("src/Main.java");
        
        when(aiService.explainCode(projectId, request.getPath()))
                .thenReturn("This is a mock explanation.");

        mockMvc.perform(post("/api/ai/explain/{projectId}", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"path\":\"src/Main.java\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.path").value("src/Main.java"))
                .andExpect(jsonPath("$.explanation").value("This is a mock explanation."));
    }

    @Test
    void testExplainCodeMissingPath() throws Exception {
        Long projectId = 1L;
        
        mockMvc.perform(post("/api/ai/explain/{projectId}", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"path\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void testExplainCodeProjectNotFound() throws Exception {
        Long projectId = 99L;
        AiExplainRequestDto request = new AiExplainRequestDto("src/Main.java");
        
        when(aiService.explainCode(projectId, request.getPath()))
                .thenThrow(new ResourceNotFoundException("Project not found with id: 99"));

        mockMvc.perform(post("/api/ai/explain/{projectId}", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"path\":\"src/Main.java\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void testExplainCodeAiException() throws Exception {
        Long projectId = 1L;
        AiExplainRequestDto request = new AiExplainRequestDto("src/Main.java");
        
        when(aiService.explainCode(projectId, request.getPath()))
                .thenThrow(new AiException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded"));

        mockMvc.perform(post("/api/ai/explain/{projectId}", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"path\":\"src/Main.java\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Rate limit exceeded"));
    }
}
