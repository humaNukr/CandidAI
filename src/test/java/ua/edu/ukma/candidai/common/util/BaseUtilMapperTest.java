package ua.edu.ukma.candidai.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class BaseUtilMapperTest {

    private final BaseUtilMapper mapper = new BaseUtilMapper() {};

    @Test
    @DisplayName("map(LocalDate) should return UTC Instant at start of day")
    void shouldMapLocalDateToInstant() {
        LocalDate date = LocalDate.of(2026, 9, 12);

        Instant result = mapper.map(date);

        assertThat(result).isEqualTo(Instant.parse("2026-09-12T00:00:00Z"));
    }

    @Test
    @DisplayName("map(LocalDate) with null should return null")
    void shouldReturnNullForNullLocalDate() {
        assertThat(mapper.map((LocalDate) null)).isNull();
    }

    @Test
    @DisplayName("map(OffsetDateTime) should return Instant")
    void shouldMapOffsetDateTimeToInstant() {
        OffsetDateTime offsetDateTime = OffsetDateTime.of(2026, 9, 12, 14, 30, 0, 0, ZoneOffset.ofHours(3));

        Instant result = mapper.map(offsetDateTime);

        assertThat(result).isEqualTo(Instant.parse("2026-09-12T11:30:00Z"));
    }

    @Test
    @DisplayName("map(OffsetDateTime) with null should return null")
    void shouldReturnNullForNullOffsetDateTime() {
        assertThat(mapper.map((OffsetDateTime) null)).isNull();
    }

    @Test
    @DisplayName("map(Instant) should return OffsetDateTime at UTC")
    void shouldMapInstantToOffsetDateTime() {
        Instant instant = Instant.parse("2026-09-12T11:30:00Z");

        OffsetDateTime result = mapper.map(instant);

        assertThat(result).isEqualTo(OffsetDateTime.of(2026, 9, 12, 11, 30, 0, 0, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("map(Instant) with null should return null")
    void shouldReturnNullForNullInstant() {
        assertThat(mapper.map((Instant) null)).isNull();
    }
}
