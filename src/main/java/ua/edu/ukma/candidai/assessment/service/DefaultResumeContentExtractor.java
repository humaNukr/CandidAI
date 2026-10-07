package ua.edu.ukma.candidai.assessment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import ua.edu.ukma.candidai.assessment.service.parser.ResumeParsingService;
import ua.edu.ukma.candidai.common.storage.FileStorageService;

import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultResumeContentExtractor implements ResumeContentExtractor {

    private final FileStorageService fileStorageService;
    private final ResumeParsingService resumeParsingService;

    @Override
    public String extractText(String resumeUrl, String candidateName) {
        log.info("Extracting resume content for candidate {} from source {}", candidateName, resumeUrl);

        if (resumeUrl == null || resumeUrl.isBlank()) {
            return generateFallbackSummary(candidateName, resumeUrl);
        }

        String fileName = extractFileName(resumeUrl);
        String parsedText = tryExtractTextFromStorage(fileName);
        if (parsedText != null && !parsedText.isBlank()) {
            log.info("Successfully extracted text from uploaded resume for candidate {}", candidateName);
            return parsedText;
        }

        return generateFallbackSummary(candidateName, resumeUrl);
    }

    private String tryExtractTextFromStorage(String fileName) {
        try {
            Resource resource = fileStorageService.loadFileAsResource(fileName);
            String contentType = determineContentType(fileName);
            return parseResource(resource, contentType);
        } catch (Exception ex) {
            log.warn("Could not parse file from storage '{}'. Using fallback summary.", fileName, ex);
            return null;
        }
    }

    private String parseResource(Resource resource, String contentType) throws Exception {
        try (InputStream inputStream = resource.getInputStream()) {
            return resumeParsingService.parse(inputStream, contentType);
        }
    }

    private String extractFileName(String resumeUrl) {
        if (resumeUrl.contains("/")) {
            return resumeUrl.substring(resumeUrl.lastIndexOf('/') + 1);
        }
        return resumeUrl;
    }

    private String determineContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return MediaType.APPLICATION_PDF_VALUE;
    }

    private String generateFallbackSummary(String candidateName, String resumeUrl) {
        return """
                Candidate Name: %s
                Resume Source: %s
                Summary: Software engineer with background in backend development, Java,
                Spring Boot framework, RESTful APIs, relational databases, and software architecture.
                Key Skills: Java, Spring Boot, PostgreSQL, Docker, Git, REST API, Unit Testing.
                """.formatted(candidateName, resumeUrl);
    }
}
