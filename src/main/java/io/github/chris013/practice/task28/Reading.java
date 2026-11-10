package io.github.chris013.practice.task28;

import java.time.Instant;
import java.util.Objects;

/** One temperature sample from one sensor. */
public record Reading(String sensorId, Instant at, double celsius) {

    public Reading {
        Objects.requireNonNull(sensorId, "sensorId");
        Objects.requireNonNull(at, "at");
        if (!Double.isFinite(celsius)) {
            throw new IllegalArgumentException("celsius must be finite, was " + celsius);
        }
    }
}
