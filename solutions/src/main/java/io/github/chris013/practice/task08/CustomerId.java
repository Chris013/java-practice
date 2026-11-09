package io.github.chris013.practice.task08;

import java.util.Objects;

/** Given. */
public record CustomerId(String value) {

    public CustomerId {
        Objects.requireNonNull(value, "value");
    }
}
