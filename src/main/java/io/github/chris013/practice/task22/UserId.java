package io.github.chris013.practice.task22;

import java.util.Objects;

/** Identifies a user. Two ids with the same value are the same user. */
public record UserId(String value) {

    public UserId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("user id must not be blank");
        }
    }
}
