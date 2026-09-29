package ua.edu.ukma.candidai.assessment.service.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfResumeParserTest {

    private PdfResumeParser parser;

    @BeforeEach
    void setUp() {
        parser = new PdfResumeParser();
    }

    @Test
    @DisplayName("supports should return true for application/pdf")
    void givenPdfContentType_supports_shouldReturnTrue() {
        assertThat(parser.supports("application/pdf")).isTrue();
        assertThat(parser.supports("APPLICATION/PDF")).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"application/json", "application/msword", "text/plain"})
    @DisplayName("supports should return false for unsupported or empty content types")
    void givenNonPdfContentType_supports_shouldReturnFalse(String contentType) {
        assertThat(parser.supports(contentType)).isFalse();
    }

    @Test
    @DisplayName("parseText should extract text correctly from valid PDF stream")
    void givenValidPdfStream_parseText_shouldExtractText() throws IOException {
        String expectedContent = "John Doe - Senior Java Engineer";
        byte[] pdfBytes = createPdfWithText(expectedContent);

        try (ByteArrayInputStream in = new ByteArrayInputStream(pdfBytes)) {
            String extracted = parser.parseText(in);
            assertThat(extracted).contains(expectedContent);
        }
    }

    @Test
    @DisplayName("parseText should throw IllegalArgumentException when given corrupted stream")
    void givenCorruptedStream_parseText_shouldThrowIllegalArgumentException() {
        byte[] corruptedData = new byte[]{0x00, 0x01, 0x02, 0x03};

        try (ByteArrayInputStream in = new ByteArrayInputStream(corruptedData)) {
            assertThatThrownBy(() -> parser.parseText(in))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageStartingWith("Could not parse PDF resume");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] createPdfWithText(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            writeText(document, page, text);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private void writeText(PDDocument document, PDPage page, String text) throws IOException {
        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
            stream.beginText();
            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            stream.newLineAtOffset(50, 700);
            stream.showText(text);
            stream.endText();
        }
    }
}
