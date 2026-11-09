package io.github.chris013.practice.task21;

import java.util.Objects;

/** A base raised to a non-negative whole-number exponent. */
public record Pow(Expr base, int exponent) implements Expr {

    public Pow {
        Objects.requireNonNull(base, "base");
        if (exponent < 0) {
            throw new IllegalArgumentException("exponent must not be negative: " + exponent);
        }
    }
}
