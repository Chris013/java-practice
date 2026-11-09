package io.github.chris013.practice.task25.domain;

import java.util.Objects;

/** Given. Identifies one subscription, for example SUB-0001. */
public record SubscriptionId(String value) {

    public SubscriptionId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("A subscription id cannot be blank");
        }
    }
}
