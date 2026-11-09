package io.github.chris013.practice.task08;

import java.util.Objects;
import java.util.UUID;

/** Given. A typed id: you can't accidentally pass a CustomerId where an OrderId is expected. */
public record OrderId(UUID value) {

    public OrderId {
        Objects.requireNonNull(value, "value");
    }

    public static OrderId random() {
        return new OrderId(UUID.randomUUID());
    }
}
