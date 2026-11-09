package io.github.chris013.practice.task21;

import java.util.Objects;

/** The product of two expressions. */
public record Mul(Expr left, Expr right) implements Expr {

    public Mul {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
    }
}
