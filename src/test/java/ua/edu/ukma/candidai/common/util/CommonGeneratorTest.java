package ua.edu.ukma.candidai.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CommonGeneratorTest {

    @Test
    @DisplayName("uuid() should generate non-null unique UUIDs")
    void shouldGenerateUniqueUuids() {
        CommonGenerator generator = new CommonGenerator();

        UUID first = generator.uuid();
        UUID second = generator.uuid();

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("now() should return current instant with default Clock")
    void shouldReturnCurrentInstant() {
        CommonGenerator generator = new CommonGenerator();
        Instant before = Instant.now().minusSeconds(1);

        Instant now = generator.now();

        assertThat(now).isAfterOrEqualTo(before);
        assertThat(now).isBeforeOrEqualTo(Instant.now().plusSeconds(1));
    }

    @Test
    @DisplayName("now() should use custom Clock when provided")
    void shouldUseCustomClock() {
        Instant fixedInstant = Instant.parse("2026-09-12T12:00:00Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);
        CommonGenerator generator = new CommonGenerator(fixedClock);

        assertThat(generator.now()).isEqualTo(fixedInstant);
    }

    @Test
    @DisplayName("constructor with null Clock should fallback to system UTC clock")
    void shouldFallbackToSystemClockWhenNullProvided() {
        CommonGenerator generator = new CommonGenerator(null);

        assertThat(generator.now()).isNotNull();
    }
}
