package ua.edu.ukma.candidai.common.actuator;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import ua.edu.ukma.candidai.common.storage.StorageProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component("resumeStorage")
@RequiredArgsConstructor
public class ResumeStorageHealthIndicator implements HealthIndicator {

    private final StorageProperties storageProperties;

    @Override
    public Health health() {
        Path storagePath = Paths.get(storageProperties.uploadDir()).toAbsolutePath().normalize();
        if (!Files.exists(storagePath)) {
            return buildDownHealth(storagePath, "Storage directory does not exist");
        }
        if (!Files.isDirectory(storagePath)) {
            return buildDownHealth(storagePath, "Storage path is not a directory");
        }
        if (!Files.isWritable(storagePath)) {
            return buildDownHealth(storagePath, "Storage directory is not writable");
        }
        try {
            long usableSpace = Files.getFileStore(storagePath).getUsableSpace();
            return Health.up()
                    .withDetail("path", storagePath.toString())
                    .withDetail("writable", true)
                    .withDetail("freeSpaceBytes", usableSpace)
                    .build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("path", storagePath.toString())
                    .build();
        }
    }

    private Health buildDownHealth(Path storagePath, String reason) {
        return Health.down()
                .withDetail("path", storagePath.toString())
                .withDetail("reason", reason)
                .build();
    }
}
