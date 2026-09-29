package ua.edu.ukma.candidai.assessment.dto;

public record UploadResumeResponse(
        String fileName,
        String fileUrl,
        String contentType,
        long sizeBytes,
        String textPreview
) {}
