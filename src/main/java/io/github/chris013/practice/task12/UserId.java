package io.github.chris013.practice.task12;

import java.util.Objects;

/** Identity of an employee who writes or reviews policy documents. */
public record UserId(String value) {

    public UserId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("A user id must not be blank");
        }
    }
}
