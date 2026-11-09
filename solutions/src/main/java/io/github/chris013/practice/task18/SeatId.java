package io.github.chris013.practice.task18;

import java.util.Objects;

/** Given. Identifies one seat in the venue, e.g. {@code "A-12"}. */
public record SeatId(String value) {

    public SeatId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("A seat id must not be blank");
        }
    }
}
