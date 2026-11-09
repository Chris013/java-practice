package io.github.chris013.practice.task21;

/**
 * A formula as an immutable tree. Task 21: read {@code tasks/21-expression-simplifier.md} first.
 *
 * <p>The sealed interface and its records form an algebraic data type: a closed set of alternatives, each with its
 * own fields. Because the set is closed, a {@code switch} over {@code Expr} can be checked for exhaustiveness.
 */
public sealed interface Expr permits Num, Var, Add, Mul, Neg, Pow {

    static Expr num(long value) {
        return new Num(value);
    }

    static Expr var(String name) {
        return new Var(name);
    }

    static Expr add(Expr left, Expr right) {
        return new Add(left, right);
    }

    static Expr mul(Expr left, Expr right) {
        return new Mul(left, right);
    }

    static Expr neg(Expr operand) {
        return new Neg(operand);
    }

    static Expr pow(Expr base, int exponent) {
        return new Pow(base, exponent);
    }
}
