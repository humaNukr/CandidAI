package ua.edu.ukma.candidai.common.actuator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.health.contributor.Health;
import ua.edu.ukma.candidai.common.storage.StorageProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;
import static ua.edu.ukma.candidai.common.actuator.ActuatorTestResources.*;

class ResumeStorageHealthIndicatorTest {

    @TempDir
    private Path tempDir;

    @Test
    void givenValidWritableDirectory_health_shouldReturnUpWithStorageDetails() {
        StorageProperties properties = new StorageProperties(tempDir.toString());
        ResumeStorageHealthIndicator indicator = new ResumeStorageHealthIndicator(properties);

        Health health = indicator.health();

        StorageHealthSnapshot actual = new StorageHealthSnapshot(
                health.getStatus().getCode(),
                (String) health.getDetails().get("path"),
                (Boolean) health.getDetails().get("writable")
        );
        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expectedUpStorageHealth(tempDir));
        assertThat((Long) health.getDetails().get("freeSpaceBytes")).isPositive();
    }

    @Test
    void givenNonExistentDirectory_health_shouldReturnDownWithMissingDirectoryReason() {
        Path missingDir = tempDir.resolve("missing-resumes");
        StorageProperties properties = new StorageProperties(missingDir.toString());
        ResumeStorageHealthIndicator indicator = new ResumeStorageHealthIndicator(properties);

        Health health = indicator.health();

        StorageHealthErrorSnapshot actual = new StorageHealthErrorSnapshot(
                health.getStatus().getCode(),
                (String) health.getDetails().get("path"),
                (String) health.getDetails().get("reason")
        );
        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expectedDownStorageHealth(missingDir, "Storage directory does not exist"));
    }

    @Test
    void givenRegularFileInsteadOfDirectory_health_shouldReturnDownWithNotDirectoryReason() throws IOException {
        Path regularFile = Files.createFile(tempDir.resolve("not-a-directory.txt"));
        StorageProperties properties = new StorageProperties(regularFile.toString());
        ResumeStorageHealthIndicator indicator = new ResumeStorageHealthIndicator(properties);

        Health health = indicator.health();

        StorageHealthErrorSnapshot actual = new StorageHealthErrorSnapshot(
                health.getStatus().getCode(),
                (String) health.getDetails().get("path"),
                (String) health.getDetails().get("reason")
        );
        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(expectedDownStorageHealth(regularFile, "Storage path is not a directory"));
    }
}
