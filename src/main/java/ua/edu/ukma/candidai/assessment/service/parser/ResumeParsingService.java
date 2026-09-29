package ua.edu.ukma.candidai.assessment.service.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeParsingService {

    private final List<ResumeParser> parsers;

    public String parse(InputStream inputStream, String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Content type must not be null or blank");
        }

        ResumeParser parser = parsers.stream()
                .filter(p -> p.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported resume format: " + contentType + ". Supported formats: PDF, DOCX"
                ));

        return parser.parseText(inputStream);
    }
}

