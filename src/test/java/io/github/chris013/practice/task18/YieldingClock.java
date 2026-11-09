package io.github.chris013.practice.task18;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A clock that always says the same time, but gives other threads a chance to run first on every reading.
 *
 * <p>This is a legitimate way to widen a race window. In production the same gap appears when a call to a database
 * or to a remote service sits between the "check" and the "act". Here we make the gap wide on purpose, so the bug
 * shows up in a test on every run instead of once in a million.
 */
final class YieldingClock extends Clock {

    private final Instant now;

    YieldingClock(Instant now) {
        this.now = now;
    }

    @Override
    public Instant instant() {
        Thread.yield();
        return now;
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
