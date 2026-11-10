package io.github.chris013.practice.task32;

import java.util.Objects;

/** The identifier our shop gives every customer account. Never blank. */
public record CustomerId(String value) {

    public CustomerId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("A customer id cannot be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
