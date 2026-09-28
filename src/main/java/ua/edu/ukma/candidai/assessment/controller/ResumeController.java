package ua.edu.ukma.candidai.assessment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ua.edu.ukma.candidai.assessment.dto.UploadResumeResponse;
import ua.edu.ukma.candidai.assessment.service.parser.ResumeParsingService;
import ua.edu.ukma.candidai.common.storage.FileStorageService;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
    private static final int PREVIEW_LENGTH = 300;

    private final FileStorageService fileStorageService;
    private final ResumeParsingService resumeParsingService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResumeResponse> uploadResume(@RequestParam("file") MultipartFile file)
            throws IOException {
        log.info("Received resume upload request: originalName={}, size={} bytes, contentType={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 10MB limit");
        }

        String storedFileName = fileStorageService.storeFile(file);
        String extractedText = resumeParsingService.parse(file.getInputStream(), file.getContentType());

        String preview = extractedText.length() > PREVIEW_LENGTH
                ? extractedText.substring(0, PREVIEW_LENGTH) + "..."
                : extractedText;

        String fileUrl = "/api/v1/resumes/download/" + storedFileName;

        UploadResumeResponse response = new UploadResumeResponse(
                storedFileName,
                fileUrl,
                file.getContentType(),
                file.getSize(),
                preview
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadResume(@PathVariable String fileName) {
        log.info("Received resume download request for file: {}", fileName);
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        MediaType mediaType = fileName.endsWith(".docx")
                ? MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                : MediaType.APPLICATION_PDF;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
