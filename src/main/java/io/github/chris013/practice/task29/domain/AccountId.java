package io.github.chris013.practice.task29.domain;

import java.util.Objects;

/** Given. Identifies one account and, in the event store, one event stream. */
public record AccountId(String value) {

    public AccountId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("account id must not be blank");
        }
    }
}
