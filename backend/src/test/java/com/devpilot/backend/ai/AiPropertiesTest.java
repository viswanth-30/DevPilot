package com.devpilot.backend.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AiPropertiesTest {

    @Test
    void testAiPropertiesGettersAndSetters() {
        AiProperties properties = new AiProperties();
        
        properties.setBaseUrl("https://api.openai.com/v1");
        properties.setKey("test-key-123");
        properties.setModel("gpt-4o");
        
        assertEquals("https://api.openai.com/v1", properties.getBaseUrl());
        assertEquals("test-key-123", properties.getKey());
        assertEquals("gpt-4o", properties.getModel());
    }
}
