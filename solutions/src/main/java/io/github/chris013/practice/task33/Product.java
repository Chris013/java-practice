package io.github.chris013.practice.task33;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A product in the warehouse catalogue.
 *
 * <p>Prices are {@link BigDecimal}. Compare them with {@code compareTo}, never with {@code equals}:
 * {@code 5.0} and {@code 5.00} are the same price but not {@code equals}.
 */
public record Product(String sku, String name, Category category, BigDecimal price, int stock, int weightGrams) {

    public Product {
        requireText(sku, "sku");
        requireText(name, "name");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(price, "price");
        if (price.signum() < 0) {
            throw new IllegalArgumentException("price must not be negative, was " + price);
        }
        if (stock < 0) {
            throw new IllegalArgumentException("stock must not be negative, was " + stock);
        }
        if (weightGrams <= 0) {
            throw new IllegalArgumentException("weightGrams must be positive, was " + weightGrams);
        }
    }

    private static void requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
