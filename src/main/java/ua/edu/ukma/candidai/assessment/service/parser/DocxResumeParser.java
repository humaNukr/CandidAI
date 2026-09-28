package ua.edu.ukma.candidai.assessment.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Set;

@Slf4j
@Component
public class DocxResumeParser implements ResumeParser {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "application/x-tika-ooxml"
    );

    @Override
    public boolean supports(String contentType) {
        if (contentType == null) {
            return false;
        }
        return SUPPORTED_TYPES.contains(contentType.toLowerCase());
    }

    @Override
    public String parseText(InputStream inputStream) {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {

            String text = extractor.getText().trim();
            log.info("Successfully parsed DOCX resume. Extracted {} characters.", text.length());
            return text;
        } catch (Exception ex) {
            log.error("Failed to parse DOCX resume: {}", ex.getMessage(), ex);
            throw new IllegalArgumentException("Could not parse DOCX resume: " + ex.getMessage(), ex);
        }
    }
}
