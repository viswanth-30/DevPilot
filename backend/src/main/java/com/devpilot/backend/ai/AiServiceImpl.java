package com.devpilot.backend.ai;

import com.devpilot.backend.dto.FileContentDto;
import com.devpilot.backend.exception.AiException;
import com.devpilot.backend.service.GitHubService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AiServiceImpl implements AiService {

    private final AiProvider aiProvider;
    private final GitHubService gitHubService;
    private final AiProperties aiProperties;

    public AiServiceImpl(AiProvider aiProvider, GitHubService gitHubService, AiProperties aiProperties) {
        this.aiProvider = aiProvider;
        this.gitHubService = gitHubService;
        this.aiProperties = aiProperties;
    }

    @Override
    public String generateResponse(String prompt) {
        return aiProvider.generateResponse(prompt);
    }

    @Override
    public String explainCode(Long projectId, String path) {
        // 1. Retrieve the file content using GitHubService
        FileContentDto fileContent = gitHubService.getFileContent(projectId, path);

        // 2. Validate file content
        if ("dir".equals(fileContent.getType()) || "tree".equals(fileContent.getType())) {
            throw new AiException(HttpStatus.BAD_REQUEST, "Requested path is a directory. Please specify a file.");
        }

        if (fileContent == null || fileContent.getContent() == null || fileContent.getContent().isBlank()) {
            throw new AiException(HttpStatus.BAD_REQUEST, "File is empty or could not be read.");
        }

        // 3. Enforce maximum file size
        byte[] contentBytes = fileContent.getContent().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (contentBytes.length > aiProperties.getMaxFileSize()) {
            throw new AiException(HttpStatus.BAD_REQUEST,
                "File is too large for AI analysis. Maximum allowed size is " + aiProperties.getMaxFileSize() + " bytes.");
        }

        // 4. Construct prompt
        String prompt = buildExplainPrompt(fileContent, path);

        // 5. Send to AI Provider
        return aiProvider.generateResponse(prompt);
    }

    private String buildExplainPrompt(FileContentDto fileContent, String path) {
        return "SYSTEM:\n" +
               "You are an expert software engineering assistant. Your task is to explain the provided source code clearly and concisely.\n" +
               "- Explain what the code does.\n" +
               "- Identify the main responsibility of the file.\n" +
               "- Explain important classes, methods, and logic.\n" +
               "- Explain important inputs and outputs.\n" +
               "- Explain important dependencies or interactions.\n" +
               "- Mention notable error handling where relevant.\n" +
               "- Keep the explanation technically accurate.\n" +
               "- Do not invent behavior that is not present in the supplied code.\n" +
               "- Do not modify the code.\n" +
               "- Do not return executable code unless necessary to explain a concept.\n" +
               "\n" +
               "USER:\n" +
               "Explain the following source file.\n" +
               "File path: " + path + "\n" +
               "\n" +
               "SOURCE CODE:\n" +
               "---BEGIN SOURCE---\n" +
               fileContent.getContent() + "\n" +
               "---END SOURCE---\n";
    }
}
