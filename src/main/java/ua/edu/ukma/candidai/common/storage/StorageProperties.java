package ua.edu.ukma.candidai.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "candidai.storage")
public record StorageProperties(
        String uploadDir
) {
    public StorageProperties {
        if (uploadDir == null || uploadDir.isBlank()) {
            uploadDir = "uploads/resumes";
        }
    }
}
