package io.github.chris013.practice.task10.domain;

import java.util.Objects;

/** Given. The barcode of one physical copy of a book. */
public record CopyId(String value) {

    public CopyId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("copy id must not be blank");
        }
    }
}
