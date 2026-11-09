package io.github.chris013.practice.task11;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Objects;

/** An {@link InstantSource} that only moves when the test says so. Safe to read from many threads. */
final class MutableInstantSource implements InstantSource {

    private volatile Instant now;

    MutableInstantSource(Instant start) {
        this.now = Objects.requireNonNull(start, "start");
    }

    void set(Instant instant) {
        now = instant;
    }

    void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public Instant instant() {
        return now;
    }
}
