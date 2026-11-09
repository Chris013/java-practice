package io.github.chris013.practice.task10.domain;

import java.util.Objects;

/** Given. The member's library card number. */
public record MemberId(String value) {

    public MemberId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("member id must not be blank");
        }
    }
}
