package io.github.chris013.practice.task18;

import java.util.Objects;

/** Given. Identifies the customer who holds or buys a seat. */
public record CustomerId(String value) {

    public CustomerId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("A customer id must not be blank");
        }
    }
}
