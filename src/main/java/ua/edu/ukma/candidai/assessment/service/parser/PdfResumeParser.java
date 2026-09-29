package ua.edu.ukma.candidai.assessment.service.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
public class PdfResumeParser implements ResumeParser {

    @Override
    public boolean supports(String contentType) {
        return MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(contentType);
    }

    @Override
    public String parseText(InputStream inputStream) {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String extractedText = stripper.getText(document).trim();
            log.info("Successfully parsed PDF resume. Extracted {} characters.", extractedText.length());
            return extractedText;
        } catch (IOException ex) {
            log.error("Failed to parse PDF resume: {}", ex.getMessage(), ex);
            throw new IllegalArgumentException("Could not parse PDF resume: " + ex.getMessage(), ex);
        }
    }
}
