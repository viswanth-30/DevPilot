package com.devpilot.backend.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class HttpAiProviderTest {

    private HttpAiProvider httpAiProvider;

    @BeforeEach
    void setUp() {
        WebClient.Builder builder = WebClient.builder();
        AiProperties properties = new AiProperties();
        properties.setBaseUrl("https://api.openai.com/v1");
        properties.setKey("test-key");
        properties.setModel("gpt-4o");
        
        httpAiProvider = new HttpAiProvider(builder, properties);
    }

    @Test
    void testGenerateResponseThrowsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> {
            httpAiProvider.generateResponse("test prompt");
        });
    }
}
