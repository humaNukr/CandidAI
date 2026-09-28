package ua.edu.ukma.candidai.common.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import ua.edu.ukma.candidai.common.exception.ResourceNotFoundException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageServiceTest {

    @TempDir
    private Path tempDir;

    private LocalFileStorageService storageService;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties(tempDir.toString());
        storageService = new LocalFileStorageService(properties);
    }

    @Test
    @DisplayName("storeFile should save file to disk and return generated unique file name")
    void givenValidMultipartFile_storeFile_shouldSaveFileAndReturnUniqueName() throws IOException {
        String originalFilename = "my_resume.pdf";
        byte[] content = "Sample resume content".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", originalFilename, "application/pdf", content);

        String storedFileName = storageService.storeFile(file);

        assertThat(storedFileName).isNotBlank();
        assertThat(storedFileName).endsWith("_" + originalFilename);

        Path savedPath = storageService.getFilePath(storedFileName);
        assertThat(Files.exists(savedPath)).isTrue();
        assertThat(Files.readAllBytes(savedPath)).isEqualTo(content);
    }

    @Test
    @DisplayName("storeFile should throw IllegalArgumentException when path traversal is attempted")
    void givenPathTraversalFileName_storeFile_shouldThrowIllegalArgumentException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../etc/passwd", "application/pdf", "malicious".getBytes()
        );

        assertThatThrownBy(() -> storageService.storeFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid file path sequence");
    }

    @Test
    @DisplayName("storeFile should throw IllegalArgumentException when file is empty or null")
    void givenEmptyFile_storeFile_shouldThrowIllegalArgumentException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]
        );

        assertThatThrownBy(() -> storageService.storeFile(emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot store empty or null file");

        assertThatThrownBy(() -> storageService.storeFile(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot store empty or null file");
    }

    @Test
    @DisplayName("loadFileAsResource should return readable resource for existing file")
    void givenExistingFile_loadFileAsResource_shouldReturnReadableResource() throws IOException {
        String fileName = "sample.pdf";
        Path target = tempDir.resolve(fileName);
        Files.writeString(target, "PDF mock data");

        Resource resource = storageService.loadFileAsResource(fileName);

        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
        assertThat(resource.getFilename()).isEqualTo(fileName);
    }

    @Test
    @DisplayName("loadFileAsResource should throw ResourceNotFoundException for missing file")
    void givenNonExistentFile_loadFileAsResource_shouldThrowResourceNotFoundException() {
        assertThatThrownBy(() -> storageService.loadFileAsResource("non_existent_file.pdf"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("File not found or not readable: non_existent_file.pdf");
    }

    @Test
    @DisplayName("getFilePath should return normalized path inside storage directory")
    void givenFileName_getFilePath_shouldReturnNormalizedPath() {
        Path path = storageService.getFilePath("document.docx");
        assertThat(path).isEqualTo(tempDir.resolve("document.docx").normalize());
    }
}
