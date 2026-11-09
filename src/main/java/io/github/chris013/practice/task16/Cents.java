package io.github.chris013.practice.task16;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Given. Money in this task is euros with exactly two decimals. A value with a third decimal is a data error,
 * so we refuse it instead of rounding it away silently.
 */
final class Cents {

    static final BigDecimal ZERO = new BigDecimal("0.00");

    private Cents() {
    }

    /** Returns the value with scale 2. Throws {@link IllegalArgumentException} if it has more than two decimals. */
    static BigDecimal of(BigDecimal value, String name) {
        Objects.requireNonNull(value, name);
        if (value.scale() > 2) {
            throw new IllegalArgumentException("%s has more than two decimals: %s".formatted(name, value));
        }
        return value.setScale(2);
    }
}
