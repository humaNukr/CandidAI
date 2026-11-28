package ua.edu.ukma.candidai.common.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ua.edu.ukma.candidai.common.util.CommonGenerator;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageDiagnosticsScheduler {

    private final StorageProperties storageProperties;
    private final CommonGenerator commonGenerator;
    private final AtomicReference<StorageDiagnosticsSnapshot> latestSnapshot = new AtomicReference<>();

    @Scheduled(
            fixedDelayString = "${candidai.scheduler.storage-diagnostics.fixed-delay-seconds:900}",
            initialDelayString = "${candidai.scheduler.storage-diagnostics.initial-delay-seconds:900}",
            timeUnit = TimeUnit.SECONDS
    )
    public void collectStorageDiagnostics() {
        Instant checkedAt = commonGenerator.now();
        Path storagePath = Paths.get(storageProperties.uploadDir()).toAbsolutePath().normalize();
        boolean exists = Files.exists(storagePath);
        boolean directory = exists && Files.isDirectory(storagePath);
        boolean writable = directory && Files.isWritable(storagePath);
        long usableSpaceBytes = resolveUsableSpace(storagePath, directory);

        StorageDiagnosticsSnapshot snapshot = new StorageDiagnosticsSnapshot(
                checkedAt,
                storagePath.toString(),
                exists,
                directory,
                writable,
                usableSpaceBytes
        );
        latestSnapshot.set(snapshot);

        log.info(
                "[SCHEDULER] StorageDiagnostics: path='{}', exists={}, directory={}, writable={}, usableSpaceBytes={}",
                snapshot.path(),
                exists,
                directory,
                writable,
                usableSpaceBytes
        );
    }

    public Optional<StorageDiagnosticsSnapshot> getLatestSnapshot() {
        return Optional.ofNullable(latestSnapshot.get());
    }

    private long resolveUsableSpace(Path storagePath, boolean directory) {
        if (!directory) {
            return 0L;
        }
        try {
            return Files.getFileStore(storagePath).getUsableSpace();
        } catch (Exception ex) {
            log.warn("[SCHEDULER] StorageDiagnostics: failed to inspect usable space for '{}': {}",
                    storagePath, ex.getMessage());
            return 0L;
        }
    }

    public record StorageDiagnosticsSnapshot(
            Instant checkedAt,
            String path,
            boolean exists,
            boolean directory,
            boolean writable,
            long usableSpaceBytes
    ) {
    }
}
