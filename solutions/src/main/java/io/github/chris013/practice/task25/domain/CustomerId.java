package io.github.chris013.practice.task25.domain;

import java.util.Objects;

/** Given. Identifies the customer who pays for a subscription. */
public record CustomerId(String value) {

    public CustomerId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("A customer id cannot be blank");
        }
    }
}
