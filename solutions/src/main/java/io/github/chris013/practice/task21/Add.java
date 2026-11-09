package io.github.chris013.practice.task21;

import java.util.Objects;

/** The sum of two expressions. Subtraction is modelled as {@code add(a, neg(b))}. */
public record Add(Expr left, Expr right) implements Expr {

    public Add {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
    }
}
