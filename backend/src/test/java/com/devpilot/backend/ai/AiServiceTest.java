package com.devpilot.backend.ai;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AiServiceTest {

    @Mock
    private AiProvider aiProvider;

    @InjectMocks
    private AiServiceImpl aiService;

    @Test
    void testAiServiceDelegatesToProvider() {
        String prompt = "Explain this code";
        String expectedResponse = "This is a mock AI response.";
        
        when(aiProvider.generateResponse(prompt)).thenReturn(expectedResponse);
        
        String actualResponse = aiService.generateResponse(prompt);
        
        assertEquals(expectedResponse, actualResponse, "Service should return the provider's response");
    }

    @Test
    void testAiProviderThrowsUnsupportedOperationException() {
        when(aiProvider.generateResponse("test")).thenThrow(new UnsupportedOperationException("Not implemented"));

        assertThrows(UnsupportedOperationException.class, () -> aiService.generateResponse("test"));
    }
}
