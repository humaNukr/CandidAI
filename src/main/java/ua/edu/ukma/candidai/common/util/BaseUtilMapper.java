package ua.edu.ukma.candidai.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public interface BaseUtilMapper {

    default Instant map(LocalDate value) {
        return value != null ? value.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
    }

    default Instant map(OffsetDateTime value) {
        return value != null ? value.toInstant() : null;
    }

    default OffsetDateTime map(Instant value) {
        return value != null ? value.atOffset(ZoneOffset.UTC) : null;
    }
}
