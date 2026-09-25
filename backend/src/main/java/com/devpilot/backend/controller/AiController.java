package com.devpilot.backend.controller;

import com.devpilot.backend.ai.AiService;
import com.devpilot.backend.dto.AiBugAnalysisResponseDto;
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

    /**
     * POST /api/ai/explain/{projectId}
     * Returns an AI-generated explanation of a source file.
     */
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

    /**
     * POST /api/ai/analyze/{projectId}
     * Returns a structured bug-analysis report for a source file.
     */
    @PostMapping("/analyze/{projectId}")
    public ResponseEntity<AiBugAnalysisResponseDto> analyzeBugs(
            @PathVariable Long projectId,
            @Valid @RequestBody AiExplainRequestDto request) {

        AiBugAnalysisResponseDto response = aiService.analyzeBugs(projectId, request.getPath());
        return ResponseEntity.ok(response);
    }
}
