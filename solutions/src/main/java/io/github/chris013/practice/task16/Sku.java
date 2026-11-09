package io.github.chris013.practice.task16;

import java.util.Objects;

/** Given. The stock keeping unit: the identity of a product in the shop. */
public record Sku(String value) {

    public Sku {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("A SKU cannot be blank");
        }
    }
}
