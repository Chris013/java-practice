package io.github.chris013.practice.task21;

import java.util.Objects;

/** The arithmetic negation of an expression. */
public record Neg(Expr operand) implements Expr {

    public Neg {
        Objects.requireNonNull(operand, "operand");
    }
}
