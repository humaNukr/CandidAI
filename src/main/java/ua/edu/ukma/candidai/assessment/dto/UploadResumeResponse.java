package ua.edu.ukma.candidai.assessment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response payload after uploading and parsing a resume")
public record UploadResumeResponse(
        @Schema(
                description = "Original stored file name",
                example = "550e8400-e29b-41d4-a716-446655440000_resume.pdf"
        )
        String fileName,

        @Schema(
                description = "Download URL for the stored file",
                example = "/api/v1/resumes/download/550e8400_resume.pdf"
        )
        String fileUrl,

        @Schema(description = "MIME content type", example = "application/pdf")
        String contentType,

        @Schema(description = "File size in bytes", example = "245760")
        long sizeBytes,

        @Schema(
                description = "Extracted text preview snippet",
                example = "Senior Java Engineer with 5+ years experience in Spring Boot..."
        )
        String textPreview
) {}
