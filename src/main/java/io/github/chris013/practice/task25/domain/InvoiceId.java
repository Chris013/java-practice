package io.github.chris013.practice.task25.domain;

import java.util.Objects;

/** Given. Identifies one invoice, for example INV-0001. */
public record InvoiceId(String value) {

    public InvoiceId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("An invoice id cannot be blank");
        }
    }
}
