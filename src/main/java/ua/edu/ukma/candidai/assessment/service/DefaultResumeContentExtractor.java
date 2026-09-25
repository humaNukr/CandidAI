package ua.edu.ukma.candidai.assessment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Default resume extractor.
 * TODO: Integrate Apache Tika / PDFBox and file storage service (S3 / Local)
 * when file uploading feature is implemented.
 */
@Slf4j
@Component
public class DefaultResumeContentExtractor implements ResumeContentExtractor {

    @Override
    public String extractText(String resumeUrl, String candidateName) {
        log.info("Extracting resume content for candidate {} from source {}", candidateName, resumeUrl);

        // TODO: Replace with binary file download and Apache Tika text parser
        return """
                Candidate Name: %s
                Resume Source: %s
                Summary: Software engineer with background in backend development, Java,
                Spring Boot framework, RESTful APIs, relational databases, and software architecture.
                Key Skills: Java, Spring Boot, PostgreSQL, Docker, Git, REST API, Unit Testing.
                """.formatted(candidateName, resumeUrl);
    }
}
