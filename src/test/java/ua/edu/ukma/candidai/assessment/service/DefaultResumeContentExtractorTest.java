package ua.edu.ukma.candidai.assessment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import ua.edu.ukma.candidai.assessment.service.parser.ResumeParsingService;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.common.storage.FileStorageService;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultResumeContentExtractorTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ResumeParsingService resumeParsingService;

    @InjectMocks
    private DefaultResumeContentExtractor extractor;

    @Test
    @DisplayName("extractText should parse stored PDF resume and return extracted text")
    void givenStoredPdfResumeUrl_extractText_shouldReturnParsedText() {
        String resumeUrl = "/api/v1/resumes/download/uuid_john_doe.pdf";
        String candidateName = "John Doe";
        String expectedParsed = "Senior Java Engineer with 5 years of Spring experience.";
        Resource mockResource = new ByteArrayResource("pdf content".getBytes());

        when(fileStorageService.loadFileAsResource("uuid_john_doe.pdf")).thenReturn(mockResource);
        when(resumeParsingService.parse(any(InputStream.class), eq(MediaType.APPLICATION_PDF_VALUE)))
                .thenReturn(expectedParsed);

        String result = extractor.extractText(resumeUrl, candidateName);

        assertThat(result).isEqualTo(expectedParsed);
        verify(fileStorageService).loadFileAsResource("uuid_john_doe.pdf");
        verify(resumeParsingService).parse(any(InputStream.class), eq(MediaType.APPLICATION_PDF_VALUE));
    }

    @Test
    @DisplayName("extractText should parse stored DOCX resume with correct content type")
    void givenStoredDocxResumeUrl_extractText_shouldParseWithDocxContentType() {
        String resumeUrl = "uuid_jane_doe.docx";
        String candidateName = "Jane Doe";
        String expectedParsed = "Lead DevOps Engineer with AWS and Kubernetes experience.";
        String expectedDocxType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        Resource mockResource = new ByteArrayResource("docx content".getBytes());

        when(fileStorageService.loadFileAsResource("uuid_jane_doe.docx")).thenReturn(mockResource);
        when(resumeParsingService.parse(any(InputStream.class), eq(expectedDocxType)))
                .thenReturn(expectedParsed);

        String result = extractor.extractText(resumeUrl, candidateName);

        assertThat(result).isEqualTo(expectedParsed);
        verify(resumeParsingService).parse(any(InputStream.class), eq(expectedDocxType));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("extractText should return fallback summary when resumeUrl is null or blank")
    void givenNullOrBlankResumeUrl_extractText_shouldReturnFallbackSummary(String resumeUrl) {
        String candidateName = "Alex Smith";

        String result = extractor.extractText(resumeUrl, candidateName);

        assertThat(result).contains("Candidate Name: Alex Smith");
        assertThat(result).contains("Key Skills: Java, Spring Boot, PostgreSQL");
        verify(fileStorageService, never()).loadFileAsResource(any());
        verify(resumeParsingService, never()).parse(any(), any());
    }

    @Test
    @DisplayName("extractText should return fallback summary gracefully when storage fails")
    void givenStorageFailure_extractText_shouldReturnFallbackSummary() {
        String resumeUrl = "missing_file.pdf";
        String candidateName = "Bob Builder";

        when(fileStorageService.loadFileAsResource("missing_file.pdf"))
                .thenThrow(new ResourceNotFoundException("File not found"));

        String result = extractor.extractText(resumeUrl, candidateName);

        assertThat(result).contains("Candidate Name: Bob Builder");
        assertThat(result).contains("Summary: Software engineer");
    }

    @Test
    @DisplayName("extractText should return fallback summary when parsed text is blank")
    void givenBlankParsedText_extractText_shouldReturnFallbackSummary() {
        String resumeUrl = "blank.pdf";
        String candidateName = "Charlie";
        Resource mockResource = new ByteArrayResource("empty".getBytes());

        when(fileStorageService.loadFileAsResource("blank.pdf")).thenReturn(mockResource);
        when(resumeParsingService.parse(any(), any())).thenReturn("   ");

        String result = extractor.extractText(resumeUrl, candidateName);

        assertThat(result).contains("Candidate Name: Charlie");
        assertThat(result).contains("Summary: Software engineer");
    }
}
