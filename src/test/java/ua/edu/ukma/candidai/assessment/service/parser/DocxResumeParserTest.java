package ua.edu.ukma.candidai.assessment.service.parser;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
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

class DocxResumeParserTest {

    private DocxResumeParser parser;

    @BeforeEach
    void setUp() {
        parser = new DocxResumeParser();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "APPLICATION/VND.OPENXMLFORMATS-OFFICEDOCUMENT.WORDPROCESSINGML.DOCUMENT",
            "application/msword",
            "application/x-tika-ooxml"
    })
    @DisplayName("supports should return true for supported Word content types")
    void givenSupportedContentTypes_supports_shouldReturnTrue(String contentType) {
        assertThat(parser.supports(contentType)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"application/pdf", "application/json", "image/png"})
    @DisplayName("supports should return false for unsupported or empty content types")
    void givenUnsupportedContentTypes_supports_shouldReturnFalse(String contentType) {
        assertThat(parser.supports(contentType)).isFalse();
    }

    @Test
    @DisplayName("parseText should extract text correctly from valid DOCX stream")
    void givenValidDocxStream_parseText_shouldExtractText() throws IOException {
        String expectedContent = "Jane Doe - Lead Architect";
        byte[] docxBytes = createDocxWithText(expectedContent);

        try (ByteArrayInputStream in = new ByteArrayInputStream(docxBytes)) {
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
                    .hasMessageStartingWith("Could not parse DOCX resume");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] createDocxWithText(String text) throws IOException {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(text);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.write(baos);
            return baos.toByteArray();
        }
    }
}
