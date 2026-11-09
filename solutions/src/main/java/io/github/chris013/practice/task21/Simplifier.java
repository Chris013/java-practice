package io.github.chris013.practice.task21;

import java.util.Objects;

/**
 * Rewrites an expression into a smaller one with the same value.
 * <p>Task 21: read {@code tasks/21-expression-simplifier.md} first.
 */
public final class Simplifier {

    private Simplifier() {}

    /**
     * Applies the rewrite rules bottom-up and repeats until a pass changes nothing (a fixed point). The result
     * evaluates to the same value as the input for every assignment of the variables.
     *
     * @throws ArithmeticException if constant folding overflows, exactly as evaluation would
     */
    public static Expr simplify(Expr expr) {
        Objects.requireNonNull(expr, "expr");
        Expr current = expr;
        while (true) {
            Expr next = pass(current);
            if (next.equals(current)) {
                return current;
            }
            current = next;
        }
    }

    /**
     * One bottom-up pass: children first, then the rules on the parent. A rule can produce a shape that another rule
     * would match (for example {@code x + x} gives {@code 2 * x}, and {@code 2 * x + 2 * x} gives {@code 2 * (2 * x)}),
     * and the parent does not see that until the next pass. That is why {@link #simplify} loops.
     */
    private static Expr pass(Expr expr) {
        return switch (expr) {
            case Num n -> n;
            case Var v -> v;
            case Neg(Expr operand) -> negation(new Neg(pass(operand)));
            case Add(Expr left, Expr right) -> sum(new Add(pass(left), pass(right)));
            case Mul(Expr left, Expr right) -> product(new Mul(pass(left), pass(right)));
            case Pow(Expr base, int exponent) -> power(new Pow(pass(base), exponent));
        };
    }

    private static Expr negation(Neg neg) {
        return switch (neg) {
            case Neg(Num(long n)) -> Expr.num(Math.negateExact(n));
            case Neg(Neg(Expr x)) -> x;
            case Neg n -> n;
        };
    }

    private static Expr sum(Add add) {
        return switch (add) {
            case Add(Num(long a), Num(long b)) -> Expr.num(Math.addExact(a, b));
            case Add(Expr x, Num(long n)) when n == 0 -> x;
            case Add(Num(long n), Expr x) when n == 0 -> x;
            // Records have structural equals, so "the same term twice" is a plain equality check.
            case Add(Expr x, Expr y) when x.equals(y) -> Expr.mul(Expr.num(2), x);
            case Add a -> a;
        };
    }

    private static Expr product(Mul mul) {
        return switch (mul) {
            case Mul(Num(long a), Num(long b)) -> Expr.num(Math.multiplyExact(a, b));
            case Mul(Num(long n), _) when n == 0 -> Expr.num(0);
            case Mul(_, Num(long n)) when n == 0 -> Expr.num(0);
            case Mul(Expr x, Num(long n)) when n == 1 -> x;
            case Mul(Num(long n), Expr x) when n == 1 -> x;
            case Mul(Num(long n), Mul(Num(long m), Expr x)) -> Expr.mul(Expr.num(Math.multiplyExact(n, m)), x);
            case Mul m -> m;
        };
    }

    private static Expr power(Pow pow) {
        return switch (pow) {
            case Pow(Num(long base), int exponent) -> Expr.num(Evaluator.exactPower(base, exponent));
            case Pow(Expr x, int exponent) when exponent == 0 -> Expr.num(1);
            case Pow(Expr x, int exponent) when exponent == 1 -> x;
            case Pow p -> p;
        };
    }
}
