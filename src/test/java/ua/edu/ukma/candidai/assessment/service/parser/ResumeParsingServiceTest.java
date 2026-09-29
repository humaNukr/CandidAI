package ua.edu.ukma.candidai.assessment.service.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeParsingServiceTest {

    @Mock
    private ResumeParser pdfParser;

    @Mock
    private ResumeParser docxParser;

    private ResumeParsingService parsingService;

    @BeforeEach
    void setUp() {
        parsingService = new ResumeParsingService(List.of(pdfParser, docxParser));
    }

    @Test
    @DisplayName("parse should delegate to matching parser when supported format is provided")
    void givenSupportedContentType_parse_shouldDelegateToMatchingParser() {
        String contentType = "application/pdf";
        InputStream stream = new ByteArrayInputStream("dummy-pdf".getBytes());
        when(pdfParser.supports(contentType)).thenReturn(true);
        when(pdfParser.parseText(stream)).thenReturn("Extracted PDF text");

        String result = parsingService.parse(stream, contentType);

        assertThat(result).isEqualTo("Extracted PDF text");
        verify(pdfParser).parseText(stream);
        verify(docxParser, never()).parseText(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("parse should throw IllegalArgumentException when contentType is null or blank")
    void givenNullOrBlankContentType_parse_shouldThrowIllegalArgumentException(String contentType) {
        InputStream stream = new ByteArrayInputStream("data".getBytes());

        assertThatThrownBy(() -> parsingService.parse(stream, contentType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Content type must not be null or blank");
    }

    @Test
    @DisplayName("parse should throw IllegalArgumentException when no parser supports the content type")
    void givenUnsupportedContentType_parse_shouldThrowIllegalArgumentException() {
        String unsupportedType = "image/png";
        InputStream stream = new ByteArrayInputStream("data".getBytes());
        when(pdfParser.supports(unsupportedType)).thenReturn(false);
        when(docxParser.supports(unsupportedType)).thenReturn(false);

        assertThatThrownBy(() -> parsingService.parse(stream, unsupportedType))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported resume format: image/png");
    }
}
