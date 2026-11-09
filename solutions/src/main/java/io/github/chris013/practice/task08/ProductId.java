package io.github.chris013.practice.task08;

import java.util.Objects;

/** Given. A product from our catalogue, e.g. "ETHIOPIA-YIRGACHEFFE-250G". */
public record ProductId(String value) {

    public ProductId {
        Objects.requireNonNull(value, "value");
    }
}
