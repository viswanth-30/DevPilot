package com.devpilot.backend.controller;

import com.devpilot.backend.ai.AiService;
import com.devpilot.backend.dto.AiExplainRequestDto;
import com.devpilot.backend.dto.AiExplainResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/explain/{projectId}")
    public ResponseEntity<AiExplainResponseDto> explainCode(
            @PathVariable Long projectId,
            @Valid @RequestBody AiExplainRequestDto request) {

        String explanation = aiService.explainCode(projectId, request.getPath());

        AiExplainResponseDto response = new AiExplainResponseDto(
                projectId,
                request.getPath(),
                explanation
        );

        return ResponseEntity.ok(response);
    }
}
