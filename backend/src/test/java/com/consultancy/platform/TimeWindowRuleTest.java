package com.consultancy.platform;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TimeWindowRuleTest {
    @Test
    void overlapFormulaTreatsAdjacentWindowsAsAvailable() {
        Instant firstStart = Instant.parse("2026-06-01T10:00:00Z");
        Instant firstEnd = Instant.parse("2026-06-01T11:00:00Z");
        Instant secondStart = Instant.parse("2026-06-01T11:00:00Z");
        Instant secondEnd = Instant.parse("2026-06-01T12:00:00Z");

        boolean overlaps = firstStart.isBefore(secondEnd) && firstEnd.isAfter(secondStart);

        assertThat(overlaps).isFalse();
    }
}
