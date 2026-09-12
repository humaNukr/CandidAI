package ua.edu.ukma.candidai.common.util;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Component
public class CommonGenerator {

    private final Clock clock;

    public CommonGenerator() {
        this(Clock.systemUTC());
    }

    public CommonGenerator(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    public UUID uuid() {
        return UUID.randomUUID();
    }

    public Instant now() {
        return clock.instant();
    }
}
