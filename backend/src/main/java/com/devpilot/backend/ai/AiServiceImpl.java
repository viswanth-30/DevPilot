package com.devpilot.backend.ai;

import org.springframework.stereotype.Service;

@Service
public class AiServiceImpl implements AiService {

    private final AiProvider aiProvider;

    public AiServiceImpl(AiProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    @Override
    public String generateResponse(String prompt) {
        return aiProvider.generateResponse(prompt);
    }
}
