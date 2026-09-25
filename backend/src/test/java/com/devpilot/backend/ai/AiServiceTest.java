package com.devpilot.backend.ai;

import com.devpilot.backend.dto.FileContentDto;
import com.devpilot.backend.exception.AiException;
import com.devpilot.backend.service.GitHubService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AiServiceTest {

    @Mock
    private AiProvider aiProvider;

    @Mock
    private GitHubService gitHubService;

    @Mock
    private AiProperties aiProperties;

    private AiServiceImpl aiService;

    @BeforeEach
    void setUp() {
        aiService = new AiServiceImpl(aiProvider, gitHubService, aiProperties);
    }

    @Test
    void testAiServiceDelegatesToProvider() {
        String prompt = "Explain this code";
        String expectedResponse = "This is a mock AI response.";

        when(aiProvider.generateResponse(prompt)).thenReturn(expectedResponse);

        String actualResponse = aiService.generateResponse(prompt);

        assertEquals(expectedResponse, actualResponse, "Service should return the provider's response");
    }

    @Test
    void testExplainCodeSuccess() {
        Long projectId = 1L;
        String path = "src/Main.java";
        FileContentDto fileContent = new FileContentDto();
        fileContent.setContent("public class Main {}");

        when(gitHubService.getFileContent(projectId, path)).thenReturn(fileContent);
        when(aiProperties.getMaxFileSize()).thenReturn(100000);
        when(aiProvider.generateResponse(anyString())).thenReturn("This is a class.");

        String explanation = aiService.explainCode(projectId, path);

        assertEquals("This is a class.", explanation);
        verify(gitHubService).getFileContent(projectId, path);
        verify(aiProvider).generateResponse(anyString());
    }

    @Test
    void testExplainCodeEmptyFileThrowsException() {
        Long projectId = 1L;
        String path = "src/Main.java";
        FileContentDto fileContent = new FileContentDto();
        fileContent.setContent("   "); // blank
        fileContent.setType("file");

        when(gitHubService.getFileContent(projectId, path)).thenReturn(fileContent);

        AiException ex = assertThrows(AiException.class, () -> aiService.explainCode(projectId, path));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    void testExplainCodeDirectoryThrowsException() {
        Long projectId = 1L;
        String path = "src";
        FileContentDto fileContent = new FileContentDto();
        fileContent.setType("dir");

        when(gitHubService.getFileContent(projectId, path)).thenReturn(fileContent);

        AiException ex = assertThrows(AiException.class, () -> aiService.explainCode(projectId, path));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("directory"));
    }

    @Test
    void testExplainCodeOversizedFileThrowsException() {
        Long projectId = 1L;
        String path = "src/Main.java";
        FileContentDto fileContent = new FileContentDto();
        fileContent.setContent("1234567890"); // 10 bytes

        when(gitHubService.getFileContent(projectId, path)).thenReturn(fileContent);
        when(aiProperties.getMaxFileSize()).thenReturn(5); // 5 bytes max

        AiException ex = assertThrows(AiException.class, () -> aiService.explainCode(projectId, path));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("too large"));
    }
}
