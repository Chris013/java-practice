package io.github.chris013.practice.task25.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock that stays on the given day, at 09:00 UTC, until the test moves it. */
final class MutableClock extends Clock {

    private LocalDate today;

    MutableClock(LocalDate today) {
        this.today = today;
    }

    void moveTo(LocalDate day) {
        today = day;
    }

    @Override
    public Instant instant() {
        return today.atTime(9, 0).toInstant(ZoneOffset.UTC);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException("Not needed in these tests");
    }
}
