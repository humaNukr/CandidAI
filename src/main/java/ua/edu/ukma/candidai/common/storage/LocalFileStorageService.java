package ua.edu.ukma.candidai.common.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@EnableConfigurationProperties(StorageProperties.class)
public class LocalFileStorageService implements FileStorageService {

    private final Path storageLocation;

    public LocalFileStorageService(StorageProperties properties) {
        this.storageLocation = Paths.get(properties.uploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create storage directory: " + this.storageLocation, ex);
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty or null file");
        }

        String originalFileName = StringUtils.cleanPath(
                Objects.requireNonNullElse(file.getOriginalFilename(), "resume.pdf")
        );

        if (originalFileName.contains("..")) {
            throw new IllegalArgumentException("Invalid file path sequence: " + originalFileName);
        }

        String storedFileName = UUID.randomUUID() + "_" + originalFileName;

        try {
            Path targetLocation = this.storageLocation.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file successfully: {} (saved as {})", originalFileName, storedFileName);
            return storedFileName;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store file " + originalFileName, ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = getFilePath(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new ResourceNotFoundException("File not found or not readable: " + fileName);
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File not found: " + fileName);
        }
    }

    @Override
    public Path getFilePath(String fileName) {
        return this.storageLocation.resolve(fileName).normalize();
    }
}
