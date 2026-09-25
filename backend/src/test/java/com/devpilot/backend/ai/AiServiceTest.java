package com.devpilot.backend.ai;

import com.devpilot.backend.dto.AiBugAnalysisResponseDto;
import com.devpilot.backend.dto.AiBugDto;
import com.devpilot.backend.dto.BugSeverity;
import com.devpilot.backend.dto.FileContentDto;
import com.devpilot.backend.exception.AiException;
import com.devpilot.backend.service.GitHubService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    // =========================================================================
    // generateResponse (delegation)
    // =========================================================================

    @Test
    void testAiServiceDelegatesToProvider() {
        String prompt = "Explain this code";
        String expectedResponse = "This is a mock AI response.";

        when(aiProvider.generateResponse(prompt)).thenReturn(expectedResponse);

        String actualResponse = aiService.generateResponse(prompt);

        assertEquals(expectedResponse, actualResponse, "Service should return the provider's response");
    }

    // =========================================================================
    // explainCode
    // =========================================================================

    @Nested
    @DisplayName("explainCode()")
    class ExplainCode {

        @Test
        void successfulExplanation() {
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
        void emptyFileThrowsException() {
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
        void directoryThrowsException() {
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
        void oversizedFileThrowsException() {
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

    // =========================================================================
    // analyzeBugs
    // =========================================================================

    @Nested
    @DisplayName("analyzeBugs()")
    class AnalyzeBugs {

        private FileContentDto fileWithContent(String content) {
            FileContentDto dto = new FileContentDto();
            dto.setContent(content);
            dto.setType("file");
            return dto;
        }

        private static final String VALID_BUG_JSON = """
                {
                  "summary": "One potential null dereference found.",
                  "bugs": [
                    {
                      "severity": "HIGH",
                      "title": "Null dereference",
                      "description": "foo() may return null.",
                      "lineReference": "42",
                      "suggestion": "Add a null check."
                    }
                  ]
                }
                """;

        private static final String EMPTY_BUGS_JSON = """
                {
                  "summary": "No significant defects identified.",
                  "bugs": []
                }
                """;

        @Test
        void successfulAnalysisReturnsParsedResponse() {
            Long projectId = 1L;
            String path = "src/Main.java";

            when(gitHubService.getFileContent(projectId, path)).thenReturn(fileWithContent("public class Main {}"));
            when(aiProperties.getMaxFileSize()).thenReturn(100000);
            when(aiProvider.generateResponse(anyString())).thenReturn(VALID_BUG_JSON);

            AiBugAnalysisResponseDto result = aiService.analyzeBugs(projectId, path);

            assertEquals(1L, result.getProjectId());
            assertEquals("src/Main.java", result.getPath());
            assertEquals("One potential null dereference found.", result.getSummary());
            assertEquals(1, result.getBugs().size());
            AiBugDto bug = result.getBugs().get(0);
            assertEquals(BugSeverity.HIGH, bug.getSeverity());
            assertEquals("Null dereference", bug.getTitle());
            assertEquals("42", bug.getLineReference());
        }

        @Test
        void emptyBugListReturnedWhenNoBugsFound() {
            Long projectId = 2L;
            String path = "src/Clean.java";

            when(gitHubService.getFileContent(projectId, path)).thenReturn(fileWithContent("class Clean {}"));
            when(aiProperties.getMaxFileSize()).thenReturn(100000);
            when(aiProvider.generateResponse(anyString())).thenReturn(EMPTY_BUGS_JSON);

            AiBugAnalysisResponseDto result = aiService.analyzeBugs(projectId, path);

            assertEquals("No significant defects identified.", result.getSummary());
            assertTrue(result.getBugs().isEmpty());
        }

        @Test
        void multipleBugsAreParsedCorrectly() {
            String multiBugJson = """
                    {
                      "summary": "Two bugs found.",
                      "bugs": [
                        {"severity": "CRITICAL", "title": "NPE", "description": "d1", "lineReference": "10", "suggestion": "s1"},
                        {"severity": "LOW",      "title": "Style", "description": "d2", "lineReference": "", "suggestion": "s2"}
                      ]
                    }
                    """;
            when(gitHubService.getFileContent(1L, "f.java")).thenReturn(fileWithContent("code"));
            when(aiProperties.getMaxFileSize()).thenReturn(100000);
            when(aiProvider.generateResponse(anyString())).thenReturn(multiBugJson);

            AiBugAnalysisResponseDto result = aiService.analyzeBugs(1L, "f.java");

            assertEquals(2, result.getBugs().size());
            assertEquals(BugSeverity.CRITICAL, result.getBugs().get(0).getSeverity());
            assertEquals(BugSeverity.LOW, result.getBugs().get(1).getSeverity());
        }

        @Test
        void unknownSeverityNormalizedToLow() {
            String unknownSeverityJson = """
                    {
                      "summary": "One bug.",
                      "bugs": [
                        {"severity": "EXTREME", "title": "T", "description": "D", "lineReference": "1", "suggestion": "S"}
                      ]
                    }
                    """;
            when(gitHubService.getFileContent(1L, "f.java")).thenReturn(fileWithContent("code"));
            when(aiProperties.getMaxFileSize()).thenReturn(100000);
            when(aiProvider.generateResponse(anyString())).thenReturn(unknownSeverityJson);

            AiBugAnalysisResponseDto result = aiService.analyzeBugs(1L, "f.java");

            assertEquals(BugSeverity.LOW, result.getBugs().get(0).getSeverity());
        }

        @Test
        void malformedJsonThrowsAiException() {
            when(gitHubService.getFileContent(1L, "f.java")).thenReturn(fileWithContent("code"));
            when(aiProperties.getMaxFileSize()).thenReturn(100000);
            when(aiProvider.generateResponse(anyString())).thenReturn("not valid json at all {{{{");

            AiException ex = assertThrows(AiException.class, () -> aiService.analyzeBugs(1L, "f.java"));
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        }

        @Test
        void emptyResponseThrowsAiException() {
            when(gitHubService.getFileContent(1L, "f.java")).thenReturn(fileWithContent("code"));
            when(aiProperties.getMaxFileSize()).thenReturn(100000);
            when(aiProvider.generateResponse(anyString())).thenReturn("   ");

            AiException ex = assertThrows(AiException.class, () -> aiService.analyzeBugs(1L, "f.java"));
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        }

        @Test
        void directoryPathThrowsException() {
            FileContentDto dir = new FileContentDto();
            dir.setType("dir");
            when(gitHubService.getFileContent(1L, "src")).thenReturn(dir);

            AiException ex = assertThrows(AiException.class, () -> aiService.analyzeBugs(1L, "src"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("directory"));
        }

        @Test
        void emptyFileThrowsException() {
            FileContentDto empty = new FileContentDto();
            empty.setContent("   ");
            empty.setType("file");
            when(gitHubService.getFileContent(1L, "f.java")).thenReturn(empty);

            AiException ex = assertThrows(AiException.class, () -> aiService.analyzeBugs(1L, "f.java"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("empty"));
        }

        @Test
        void oversizedFileThrowsException() {
            FileContentDto big = new FileContentDto();
            big.setContent("1234567890");
            when(gitHubService.getFileContent(1L, "f.java")).thenReturn(big);
            when(aiProperties.getMaxFileSize()).thenReturn(5);

            AiException ex = assertThrows(AiException.class, () -> aiService.analyzeBugs(1L, "f.java"));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertTrue(ex.getMessage().contains("too large"));
        }

        @Test
        void aiProviderFailurePropagates() {
            when(gitHubService.getFileContent(1L, "f.java")).thenReturn(fileWithContent("code"));
            when(aiProperties.getMaxFileSize()).thenReturn(100000);
            when(aiProvider.generateResponse(anyString()))
                    .thenThrow(new AiException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded"));

            AiException ex = assertThrows(AiException.class, () -> aiService.analyzeBugs(1L, "f.java"));
            assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
        }

        @Test
        void githubFailurePropagates() {
            when(gitHubService.getFileContent(1L, "f.java"))
                    .thenThrow(new com.devpilot.backend.exception.ResourceNotFoundException("Project not found with id: 1"));

            assertThrows(com.devpilot.backend.exception.ResourceNotFoundException.class,
                    () -> aiService.analyzeBugs(1L, "f.java"));
        }
    }

    // =========================================================================
    // parseBugAnalysisResponse (unit tests for parser directly)
    // =========================================================================

    @Nested
    @DisplayName("parseBugAnalysisResponse()")
    class ParseBugAnalysisResponse {

        @Test
        void parsesMarkdownFencedJson() {
            String fenced = "```json\n{\"summary\":\"ok\",\"bugs\":[]}\n```";
            AiBugAnalysisResponseDto result = aiService.parseBugAnalysisResponse(1L, "f.java", fenced);
            assertEquals("ok", result.getSummary());
            assertTrue(result.getBugs().isEmpty());
        }

        @Test
        void parsesPlainJson() {
            String plain = "{\"summary\":\"done\",\"bugs\":[]}";
            AiBugAnalysisResponseDto result = aiService.parseBugAnalysisResponse(1L, "f.java", plain);
            assertEquals("done", result.getSummary());
        }

        @Test
        void nullSeverityNormalizedToLow() {
            String json = "{\"summary\":\"s\",\"bugs\":[{\"severity\":null,\"title\":\"t\",\"description\":\"d\",\"lineReference\":\"\",\"suggestion\":\"sg\"}]}";
            AiBugAnalysisResponseDto result = aiService.parseBugAnalysisResponse(1L, "f.java", json);
            assertEquals(BugSeverity.LOW, result.getBugs().get(0).getSeverity());
        }

        @Test
        void promptContainsBugDetectionInstructions() {
            // Verify prompt includes the instruction keywords
            FileContentDto file = new FileContentDto();
            file.setContent("public class X {}");
            when(gitHubService.getFileContent(1L, "X.java")).thenReturn(file);
            when(aiProperties.getMaxFileSize()).thenReturn(100000);

            // Capture the prompt that's sent to the provider
            when(aiProvider.generateResponse(anyString())).thenAnswer(invocation -> {
                String prompt = invocation.getArgument(0);
                assertTrue(prompt.contains("bug"), "Prompt must mention bug detection");
                assertTrue(prompt.contains("---BEGIN SOURCE---"), "Prompt must include source delimiter");
                assertTrue(prompt.contains("---END SOURCE---"), "Prompt must include source delimiter");
                assertTrue(prompt.contains("X.java"), "Prompt must include file path");
                assertTrue(prompt.contains("public class X {}"), "Prompt must include source code");
                assertTrue(prompt.contains("CRITICAL"), "Prompt must list severity levels");
                return "{\"summary\":\"ok\",\"bugs\":[]}";
            });

            aiService.analyzeBugs(1L, "X.java");
        }
    }

    // =========================================================================
    // suggestImprovements
    // =========================================================================

    @Nested
    @DisplayName("suggestImprovements()")
    class SuggestImprovements {

        private FileContentDto fileWithContent(String content) {
            FileContentDto dto = new FileContentDto();
            dto.setContent(content);
            dto.setType("file");
            return dto;
        }

        private static final String VALID_IMPROVEMENT_JSON = """
                {
                  "summary": "Several improvements could make this service easier to maintain.",
                  "suggestions": [
                    {
                      "category": "MAINTAINABILITY",
                      "priority": "MEDIUM",
                      "title": "Extract repeated mapping logic",
                      "description": "The response mapping logic could be isolated...",
                      "lineReference": "42-55",
                      "recommendation": "Move the mapping into a dedicated method..."
                    }
                  ]
                }
                """;

        private static final String EMPTY_IMPROVEMENT_JSON = """
                {
                  "summary": "No significant improvement opportunities were identified.",
                  "suggestions": []
                }
                """;

        @Test
        void successfulAnalysisReturnsParsedResponse() {
            Long projectId = 1L;
            String path = "src/Main.java";

            when(gitHubService.getFileContent(projectId, path)).thenReturn(fileWithContent("public class Main {}"));
            when(aiProperties.getMaxFileSize()).thenReturn(100000);
            when(aiProvider.generateResponse(anyString())).thenReturn(VALID_IMPROVEMENT_JSON);

            com.devpilot.backend.dto.AiImprovementResponseDto result = aiService.suggestImprovements(projectId, path);

            assertEquals(1L, result.getProjectId());
            assertEquals("src/Main.java", result.getPath());
            assertEquals("Several improvements could make this service easier to maintain.", result.getSummary());
            assertEquals(1, result.getSuggestions().size());
            com.devpilot.backend.dto.AiImprovementDto suggestion = result.getSuggestions().get(0);
            assertEquals(com.devpilot.backend.dto.ImprovementCategory.MAINTAINABILITY, suggestion.getCategory());
            assertEquals(com.devpilot.backend.dto.ImprovementPriority.MEDIUM, suggestion.getPriority());
            assertEquals("Extract repeated mapping logic", suggestion.getTitle());
            assertEquals("42-55", suggestion.getLineReference());
        }

        @Test
        void emptySuggestionListReturnedWhenNoImprovementsFound() {
            Long projectId = 2L;
            String path = "src/Clean.java";

            when(gitHubService.getFileContent(projectId, path)).thenReturn(fileWithContent("class Clean {}"));
            when(aiProperties.getMaxFileSize()).thenReturn(100000);
            when(aiProvider.generateResponse(anyString())).thenReturn(EMPTY_IMPROVEMENT_JSON);

            com.devpilot.backend.dto.AiImprovementResponseDto result = aiService.suggestImprovements(projectId, path);

            assertEquals("No significant improvement opportunities were identified.", result.getSummary());
            assertTrue(result.getSuggestions().isEmpty());
        }

        @Test
        void multipleSuggestionsAreParsedCorrectly() {
            String multiJson = """
                    {
                      "summary": "Two improvements found.",
                      "suggestions": [
                        {"category": "READABILITY", "priority": "LOW", "title": "T1", "description": "D1", "lineReference": "1", "recommendation": "R1"},
                        {"category": "SECURITY", "priority": "HIGH", "title": "T2", "description": "D2", "lineReference": "2", "recommendation": "R2"}
                      ]
                    }
                    """;
            when(gitHubService.getFileContent(1L, "f.java")).thenReturn(fileWithContent("code"));
            when(aiProperties.getMaxFileSize()).thenReturn(100000);
            when(aiProvider.generateResponse(anyString())).thenReturn(multiJson);

            com.devpilot.backend.dto.AiImprovementResponseDto result = aiService.suggestImprovements(1L, "f.java");

            assertEquals(2, result.getSuggestions().size());
            assertEquals(com.devpilot.backend.dto.ImprovementCategory.READABILITY, result.getSuggestions().get(0).getCategory());
            assertEquals(com.devpilot.backend.dto.ImprovementPriority.LOW, result.getSuggestions().get(0).getPriority());
            assertEquals(com.devpilot.backend.dto.ImprovementCategory.SECURITY, result.getSuggestions().get(1).getCategory());
            assertEquals(com.devpilot.backend.dto.ImprovementPriority.HIGH, result.getSuggestions().get(1).getPriority());
        }

        @Test
        void unknownCategoryAndPriorityNormalized() {
            String unknownJson = """
                    {
                      "summary": "One improvement.",
                      "suggestions": [
                        {"category": "UNKNOWN_CAT", "priority": "UNKNOWN_PRI", "title": "T", "description": "D", "lineReference": "1", "recommendation": "S"}
                      ]
                    }
                    """;
            when(gitHubService.getFileContent(1L, "f.java")).thenReturn(fileWithContent("code"));
            when(aiProperties.getMaxFileSize()).thenReturn(100000);
            when(aiProvider.generateResponse(anyString())).thenReturn(unknownJson);

            com.devpilot.backend.dto.AiImprovementResponseDto result = aiService.suggestImprovements(1L, "f.java");

            assertEquals(com.devpilot.backend.dto.ImprovementCategory.MAINTAINABILITY, result.getSuggestions().get(0).getCategory());
            assertEquals(com.devpilot.backend.dto.ImprovementPriority.LOW, result.getSuggestions().get(0).getPriority());
        }

        @Test
        void malformedJsonThrowsAiException() {
            when(gitHubService.getFileContent(1L, "f.java")).thenReturn(fileWithContent("code"));
            when(aiProperties.getMaxFileSize()).thenReturn(100000);
            when(aiProvider.generateResponse(anyString())).thenReturn("not valid json at all {{{{");

            AiException ex = assertThrows(AiException.class, () -> aiService.suggestImprovements(1L, "f.java"));
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        }

        @Test
        void promptContainsImprovementInstructions() {
            FileContentDto file = new FileContentDto();
            file.setContent("public class X {}");
            when(gitHubService.getFileContent(1L, "X.java")).thenReturn(file);
            when(aiProperties.getMaxFileSize()).thenReturn(100000);

            when(aiProvider.generateResponse(anyString())).thenAnswer(invocation -> {
                String prompt = invocation.getArgument(0);
                assertTrue(prompt.contains("IMPROVEMENT:"), "Prompt must define improvement");
                assertTrue(prompt.contains("---BEGIN SOURCE---"), "Prompt must include source delimiter");
                assertTrue(prompt.contains("X.java"), "Prompt must include file path");
                return "{\"summary\":\"ok\",\"suggestions\":[]}";
            });

            aiService.suggestImprovements(1L, "X.java");
        }
    }

    // =========================================================================
    // suggestTests
    // =========================================================================

    @Nested
    @DisplayName("suggestTests()")
    class SuggestTests {

        private FileContentDto fileWithContent(String content) {
            FileContentDto dto = new FileContentDto();
            dto.setContent(content);
            dto.setType("file");
            return dto;
        }

        private static final String VALID_TEST_JSON = """
                {
                  "summary": "The file should be covered with unit tests for CRUD operations.",
                  "tests": [
                    {
                      "testType": "UNIT",
                      "priority": "HIGH",
                      "title": "Should create project successfully",
                      "description": "Verify that a valid project request creates a project.",
                      "targetMethod": "createProject",
                      "scenario": "Valid project request",
                      "expectedBehavior": "Project is saved."
                    }
                  ]
                }
                """;

        private static final String EMPTY_TEST_JSON = """
                {
                  "summary": "No tests are required for this code.",
                  "tests": []
                }
                """;

        @Test
        void successfulAnalysisReturnsParsedResponse() {
            Long projectId = 1L;
            String path = "src/Main.java";

            when(gitHubService.getFileContent(projectId, path)).thenReturn(fileWithContent("public class Main {}"));
            when(aiProperties.getMaxFileSize()).thenReturn(100000);
            when(aiProvider.generateResponse(anyString())).thenReturn(VALID_TEST_JSON);

            com.devpilot.backend.dto.AiTestSuggestionResponseDto result = aiService.suggestTests(projectId, path);

            assertEquals(1L, result.getProjectId());
            assertEquals("src/Main.java", result.getPath());
            assertEquals("The file should be covered with unit tests for CRUD operations.", result.getSummary());
            assertEquals(1, result.getTests().size());
            com.devpilot.backend.dto.AiTestSuggestionDto suggestion = result.getTests().get(0);
            assertEquals(com.devpilot.backend.dto.TestType.UNIT, suggestion.getTestType());
            assertEquals(com.devpilot.backend.dto.TestPriority.HIGH, suggestion.getPriority());
            assertEquals("Should create project successfully", suggestion.getTitle());
            assertEquals("createProject", suggestion.getTargetMethod());
        }

        @Test
        void emptySuggestionListReturnedWhenNoTestsFound() {
            Long projectId = 2L;
            String path = "src/Clean.java";

            when(gitHubService.getFileContent(projectId, path)).thenReturn(fileWithContent("interface Clean {}"));
            when(aiProperties.getMaxFileSize()).thenReturn(100000);
            when(aiProvider.generateResponse(anyString())).thenReturn(EMPTY_TEST_JSON);

            com.devpilot.backend.dto.AiTestSuggestionResponseDto result = aiService.suggestTests(projectId, path);

            assertEquals("No tests are required for this code.", result.getSummary());
            assertTrue(result.getTests().isEmpty());
        }

        @Test
        void multipleSuggestionsAreParsedCorrectly() {
            String multiJson = """
                    {
                      "summary": "Two tests found.",
                      "tests": [
                        {"testType": "EDGE_CASE", "priority": "LOW", "title": "T1", "description": "D1", "targetMethod": "M1", "scenario": "S1", "expectedBehavior": "E1"},
                        {"testType": "ERROR_HANDLING", "priority": "HIGH", "title": "T2", "description": "D2", "targetMethod": "M2", "scenario": "S2", "expectedBehavior": "E2"}
                      ]
                    }
                    """;
            when(gitHubService.getFileContent(1L, "f.java")).thenReturn(fileWithContent("code"));
            when(aiProperties.getMaxFileSize()).thenReturn(100000);
            when(aiProvider.generateResponse(anyString())).thenReturn(multiJson);

            com.devpilot.backend.dto.AiTestSuggestionResponseDto result = aiService.suggestTests(1L, "f.java");

            assertEquals(2, result.getTests().size());
            assertEquals(com.devpilot.backend.dto.TestType.EDGE_CASE, result.getTests().get(0).getTestType());
            assertEquals(com.devpilot.backend.dto.TestPriority.LOW, result.getTests().get(0).getPriority());
            assertEquals(com.devpilot.backend.dto.TestType.ERROR_HANDLING, result.getTests().get(1).getTestType());
            assertEquals(com.devpilot.backend.dto.TestPriority.HIGH, result.getTests().get(1).getPriority());
        }

        @Test
        void unknownTestTypeAndPriorityNormalized() {
            String unknownJson = """
                    {
                      "summary": "One test.",
                      "tests": [
                        {"testType": "UNKNOWN_TYPE", "priority": "UNKNOWN_PRI", "title": "T", "description": "D", "targetMethod": "M", "scenario": "S", "expectedBehavior": "E"}
                      ]
                    }
                    """;
            when(gitHubService.getFileContent(1L, "f.java")).thenReturn(fileWithContent("code"));
            when(aiProperties.getMaxFileSize()).thenReturn(100000);
            when(aiProvider.generateResponse(anyString())).thenReturn(unknownJson);

            com.devpilot.backend.dto.AiTestSuggestionResponseDto result = aiService.suggestTests(1L, "f.java");

            assertEquals(com.devpilot.backend.dto.TestType.UNIT, result.getTests().get(0).getTestType());
            assertEquals(com.devpilot.backend.dto.TestPriority.LOW, result.getTests().get(0).getPriority());
        }

        @Test
        void malformedJsonThrowsAiException() {
            when(gitHubService.getFileContent(1L, "f.java")).thenReturn(fileWithContent("code"));
            when(aiProperties.getMaxFileSize()).thenReturn(100000);
            when(aiProvider.generateResponse(anyString())).thenReturn("not valid json at all {{{{");

            AiException ex = assertThrows(AiException.class, () -> aiService.suggestTests(1L, "f.java"));
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        }

        @Test
        void promptContainsTestInstructions() {
            FileContentDto file = new FileContentDto();
            file.setContent("public class X {}");
            when(gitHubService.getFileContent(1L, "X.java")).thenReturn(file);
            when(aiProperties.getMaxFileSize()).thenReturn(100000);

            when(aiProvider.generateResponse(anyString())).thenAnswer(invocation -> {
                String prompt = invocation.getArgument(0);
                assertTrue(prompt.contains("unit tests"), "Prompt must mention unit tests");
                assertTrue(prompt.contains("---BEGIN SOURCE---"), "Prompt must include source delimiter");
                assertTrue(prompt.contains("X.java"), "Prompt must include file path");
                return "{\"summary\":\"ok\",\"tests\":[]}";
            });

            aiService.suggestTests(1L, "X.java");
        }
    }
}
