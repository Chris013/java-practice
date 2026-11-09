package io.github.chris013.practice.task21;

import java.util.Objects;

/**
 * Turns an expression tree back into readable text with as few parentheses as possible.
 * <p>Task 21: read {@code tasks/21-expression-simplifier.md} first.
 */
public final class Printer {

    // Higher number = binds tighter. A child is parenthesized when it binds looser than its slot requires.
    private static final int ADD = 1;
    private static final int MUL = 2;
    private static final int NEG = 3;
    private static final int POW = 4;
    private static final int ATOM = 5;

    private Printer() {}

    /**
     * Examples: {@code x + 1}, {@code 2 * (x + 1)}, {@code -(x + 1)}, {@code (-x)^2}, {@code a + (b + c)}.
     */
    public static String print(Expr expr) {
        Objects.requireNonNull(expr, "expr");
        return switch (expr) {
            case Num(long value) -> Long.toString(value);
            case Var(String name) -> name;
            // Add and Mul are left-associative: the left child may have the same precedence without parentheses,
            // the right child may not, so a + (b + c) keeps the structure of the tree visible.
            case Add(Expr left, Expr right) -> printAt(left, ADD) + " + " + printAt(right, MUL);
            case Mul(Expr left, Expr right) -> printAt(left, MUL) + " * " + printAt(right, NEG);
            case Neg(Expr operand) -> "-" + printAt(operand, POW);
            case Pow(Expr base, int exponent) -> printAt(base, ATOM) + "^" + exponent;
        };
    }

    private static String printAt(Expr expr, int minPrecedence) {
        String text = print(expr);
        return precedence(expr) < minPrecedence ? "(" + text + ")" : text;
    }

    /** A negative number reads like a negation, so it gets the negation's precedence. */
    private static int precedence(Expr expr) {
        return switch (expr) {
            case Add ignored -> ADD;
            case Mul ignored -> MUL;
            case Neg ignored -> NEG;
            case Pow ignored -> POW;
            case Num(long value) -> value < 0 ? NEG : ATOM;
            case Var ignored -> ATOM;
        };
    }
}
