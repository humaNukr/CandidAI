package ua.edu.ukma.candidai.assessment.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.edu.ukma.candidai.assessment.service.parser.ResumeParsingService;
import ua.edu.ukma.candidai.common.exception.GlobalExceptionHandler;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;
import ua.edu.ukma.candidai.common.storage.FileStorageService;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResumeController.class)
@Import(GlobalExceptionHandler.class)
class ResumeControllerTest {

    private static final String UPLOAD_URL = "/api/v1/resumes/upload";
    private static final String DOWNLOAD_URL_PREFIX = "/api/v1/resumes/download/";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileStorageService fileStorageService;

    @MockitoBean
    private ResumeParsingService resumeParsingService;

    @Test
    @DisplayName("POST /api/v1/resumes/upload - should return 200 Ok with file metadata and preview")
    void givenValidFile_uploadResume_shouldReturn200OkWithPreview() throws Exception {
        byte[] content = "Software Engineer Resume Content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "my_resume.pdf", MediaType.APPLICATION_PDF_VALUE, content
        );
        String storedFileName = "uuid_my_resume.pdf";
        String parsedText = "Parsed text from resume with skills and experience.";

        when(fileStorageService.storeFile(any())).thenReturn(storedFileName);
        when(resumeParsingService.parse(any(), eq(MediaType.APPLICATION_PDF_VALUE))).thenReturn(parsedText);

        mockMvc.perform(multipart(UPLOAD_URL).file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value(storedFileName))
                .andExpect(jsonPath("$.fileUrl").value(DOWNLOAD_URL_PREFIX + storedFileName))
                .andExpect(jsonPath("$.contentType").value(MediaType.APPLICATION_PDF_VALUE))
                .andExpect(jsonPath("$.sizeBytes").value(content.length))
                .andExpect(jsonPath("$.textPreview").value(parsedText));

        verify(fileStorageService).storeFile(any());
        verify(resumeParsingService).parse(any(), eq(MediaType.APPLICATION_PDF_VALUE));
    }

    @Test
    @DisplayName("POST /api/v1/resumes/upload - should truncate text preview when longer than 300 characters")
    void givenLongText_uploadResume_shouldTruncatePreviewWithEllipsis() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", MediaType.APPLICATION_PDF_VALUE, "dummy".getBytes()
        );
        String storedFileName = "uuid_resume.pdf";
        String longText = "A".repeat(350);

        when(fileStorageService.storeFile(any())).thenReturn(storedFileName);
        when(resumeParsingService.parse(any(), any())).thenReturn(longText);

        mockMvc.perform(multipart(UPLOAD_URL).file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.textPreview").value(endsWith("...")));
    }

    @Test
    @DisplayName("POST /api/v1/resumes/upload - should return 400 Bad Request when file is empty")
    void givenEmptyFile_uploadResume_shouldReturn400BadRequest() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0]
        );

        mockMvc.perform(multipart(UPLOAD_URL).file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Cannot upload empty file"));
    }

    @Test
    @DisplayName("GET /api/v1/resumes/download/{fileName} - should return 200 Ok with PDF content")
    void givenExistingPdf_downloadResume_shouldReturn200OkWithPdfContent() throws Exception {
        String fileName = "sample.pdf";
        byte[] pdfBytes = "mock pdf data".getBytes();
        Resource resource = new ByteArrayResource(pdfBytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        when(fileStorageService.loadFileAsResource(fileName)).thenReturn(resource);

        mockMvc.perform(get(DOWNLOAD_URL_PREFIX + fileName))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"sample.pdf\""))
                .andExpect(content().bytes(pdfBytes));

        verify(fileStorageService).loadFileAsResource(fileName);
    }

    @Test
    @DisplayName("GET /api/v1/resumes/download/{fileName} - should return 200 Ok with DOCX content type")
    void givenExistingDocx_downloadResume_shouldReturn200OkWithDocxContentType() throws Exception {
        String fileName = "sample.docx";
        byte[] docxBytes = "mock docx data".getBytes();
        Resource resource = new ByteArrayResource(docxBytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        when(fileStorageService.loadFileAsResource(fileName)).thenReturn(resource);

        mockMvc.perform(get(DOWNLOAD_URL_PREFIX + fileName))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                ))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"sample.docx\""))
                .andExpect(content().bytes(docxBytes));

        verify(fileStorageService).loadFileAsResource(fileName);
    }

    @Test
    @DisplayName("GET /api/v1/resumes/download/{fileName} - should return 404 Not Found when file does not exist")
    void givenMissingFile_downloadResume_shouldReturn404NotFound() throws Exception {
        String fileName = "missing.pdf";
        when(fileStorageService.loadFileAsResource(fileName))
                .thenThrow(new ResourceNotFoundException("File not found or not readable: " + fileName));

        mockMvc.perform(get(DOWNLOAD_URL_PREFIX + fileName))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(fileStorageService).loadFileAsResource(fileName);
    }
}
