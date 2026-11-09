package io.github.chris013.practice.task21;

import java.util.Map;
import java.util.Objects;

/**
 * Computes the value of an expression with exact {@code long} arithmetic.
 * <p>Task 21: read {@code tasks/21-expression-simplifier.md} first.
 */
public final class Evaluator {

    private Evaluator() {}

    /**
     * @throws IllegalArgumentException if the expression uses a variable that is not in {@code variables}
     * @throws ArithmeticException if an intermediate result does not fit into a {@code long}
     */
    public static long evaluate(Expr expr, Map<String, Long> variables) {
        Objects.requireNonNull(expr, "expr");
        Objects.requireNonNull(variables, "variables");
        return switch (expr) {
            case Num(long value) -> value;
            case Var(String name) -> lookup(name, variables);
            case Add(Expr left, Expr right) -> Math.addExact(evaluate(left, variables), evaluate(right, variables));
            case Mul(Expr left, Expr right) -> Math.multiplyExact(evaluate(left, variables), evaluate(right, variables));
            case Neg(Expr operand) -> Math.negateExact(evaluate(operand, variables));
            case Pow(Expr base, int exponent) -> exactPower(evaluate(base, variables), exponent);
        };
    }

    private static long lookup(String name, Map<String, Long> variables) {
        Long value = variables.get(name);
        if (value == null) {
            throw new IllegalArgumentException("unknown variable: " + name);
        }
        return value;
    }

    /**
     * Integer power by repeated squaring, with overflow checks. It squares only when a higher bit of the exponent
     * is still to come, so it never squares a value larger than the final result. That keeps {@code 2^62} valid
     * while {@code 2^63} throws, and {@code 1^1000000000} takes about 30 steps instead of a billion multiplications.
     */
    static long exactPower(long base, int exponent) {
        long result = 1;
        long factor = base;
        int remaining = exponent;
        while (remaining > 0) {
            if ((remaining & 1) == 1) {
                result = Math.multiplyExact(result, factor);
            }
            remaining >>= 1;
            if (remaining > 0) {
                factor = Math.multiplyExact(factor, factor);
            }
        }
        return result;
    }
}
