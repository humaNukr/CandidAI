package ua.edu.ukma.candidai.common.storage;

import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ua.edu.ukma.candidai.common.storage.StorageDiagnosticsScheduler.StorageDiagnosticsSnapshot;
import ua.edu.ukma.candidai.common.util.CommonGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Unit tests for StorageDiagnosticsScheduler")
class StorageDiagnosticsSchedulerTest {

    private static final Instant FIXED_NOW = Instant.parse("2026-11-28T10:00:00Z");

    @TempDir
    private Path tempDir;

    @Test
    @DisplayName("collectStorageDiagnostics - should capture writable status and usable space for existing dir")
    void collectStorageDiagnostics_WhenDirectoryExists_ShouldRecordHealthySnapshot() {
        CommonGenerator commonGenerator = mock(CommonGenerator.class);
        when(commonGenerator.now()).thenReturn(FIXED_NOW);
        StorageDiagnosticsScheduler scheduler = new StorageDiagnosticsScheduler(
                new StorageProperties(tempDir.toString()),
                commonGenerator
        );

        scheduler.collectStorageDiagnostics();

        StorageDiagnosticsSnapshot snapshot = scheduler.getLatestSnapshot().orElseThrow();
        assertThat(snapshot.checkedAt()).isEqualTo(FIXED_NOW);
        assertThat(snapshot.path()).isEqualTo(tempDir.toAbsolutePath().normalize().toString());
        assertThat(snapshot.exists()).isTrue();
        assertThat(snapshot.directory()).isTrue();
        assertThat(snapshot.writable()).isTrue();
        assertThat(snapshot.usableSpaceBytes()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("collectStorageDiagnostics - should safely record missing directory without throwing")
    void collectStorageDiagnostics_WhenDirectoryMissing_ShouldRecordMissingSnapshot() {
        CommonGenerator commonGenerator = mock(CommonGenerator.class);
        when(commonGenerator.now()).thenReturn(FIXED_NOW);
        Path missingPath = tempDir.resolve("missing-uploads");
        StorageDiagnosticsScheduler scheduler = new StorageDiagnosticsScheduler(
                new StorageProperties(missingPath.toString()),
                commonGenerator
        );

        scheduler.collectStorageDiagnostics();

        StorageDiagnosticsSnapshot snapshot = scheduler.getLatestSnapshot().orElseThrow();
        assertThat(snapshot.exists()).isFalse();
        assertThat(snapshot.directory()).isFalse();
        assertThat(snapshot.writable()).isFalse();
        assertThat(snapshot.usableSpaceBytes()).isZero();
    }
}
