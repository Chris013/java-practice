package io.github.chris013.practice.task12;

import java.util.Objects;

/** Identity of a policy document, for example {@code POL-042}. */
public record DocumentId(String value) {

    public DocumentId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("A document id must not be blank");
        }
    }
}
