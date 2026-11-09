package io.github.chris013.practice.task21;

import static io.github.chris013.practice.task21.Expr.add;
import static io.github.chris013.practice.task21.Expr.mul;
import static io.github.chris013.practice.task21.Expr.neg;
import static io.github.chris013.practice.task21.Expr.num;
import static io.github.chris013.practice.task21.Expr.pow;
import static io.github.chris013.practice.task21.Expr.var;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SimplifierTest {

    private static final Expr X = var("x");
    private static final Expr Y = var("y");

    private static Expr simplify(Expr expr) {
        return Simplifier.simplify(expr);
    }

    @Nested
    class ConstantFolding {

        @Test
        void folds_arithmetic_on_numbers() {
            assertThat(simplify(add(num(2), num(3)))).isEqualTo(num(5));
            assertThat(simplify(mul(num(2), num(3)))).isEqualTo(num(6));
            assertThat(simplify(neg(num(4)))).isEqualTo(num(-4));
            assertThat(simplify(pow(num(-2), 3))).isEqualTo(num(-8));
        }

        @Test
        void folds_nested_constants_bottom_up() {
            assertThat(simplify(add(mul(num(2), num(3)), pow(num(2), 2)))).isEqualTo(num(10));
        }

        @Test
        void an_overflowing_fold_throws_like_evaluation_does() {
            assertThatThrownBy(() -> simplify(add(num(Long.MAX_VALUE), num(1))))
                    .isInstanceOf(ArithmeticException.class);
        }

        @Test
        void a_negation_that_does_not_fit_throws() {
            assertThatThrownBy(() -> simplify(neg(num(Long.MIN_VALUE)))).isInstanceOf(ArithmeticException.class);
        }

        @Test
        void a_grouped_constant_that_does_not_fit_throws() {
            assertThatThrownBy(() -> simplify(mul(num(Long.MAX_VALUE), mul(num(2), X))))
                    .isInstanceOf(ArithmeticException.class);
        }
    }

    @Nested
    class Identities {

        @Test
        void adding_zero_on_either_side_gives_the_other_side() {
            assertThat(simplify(add(X, num(0)))).isEqualTo(X);
            assertThat(simplify(add(num(0), X))).isEqualTo(X);
            assertThat(simplify(add(mul(X, Y), num(0)))).isEqualTo(mul(X, Y));
        }

        @Test
        void multiplying_by_one_on_either_side_gives_the_other_side() {
            assertThat(simplify(mul(X, num(1)))).isEqualTo(X);
            assertThat(simplify(mul(num(1), X))).isEqualTo(X);
        }

        @Test
        void multiplying_by_zero_on_either_side_gives_zero() {
            assertThat(simplify(mul(X, num(0)))).isEqualTo(num(0));
            assertThat(simplify(mul(num(0), add(X, Y)))).isEqualTo(num(0));
        }

        @Test
        void a_double_negation_is_removed() {
            assertThat(simplify(neg(neg(X)))).isEqualTo(X);
        }

        @Test
        void a_power_of_zero_is_one_and_a_power_of_one_is_the_base() {
            assertThat(simplify(pow(X, 0))).isEqualTo(num(1));
            assertThat(simplify(pow(X, 1))).isEqualTo(X);
        }

        @Test
        void zero_to_a_positive_power_is_zero() {
            assertThat(simplify(pow(num(0), 5))).isEqualTo(num(0));
        }
    }

    @Nested
    class SelfAddition {

        @Test
        void adding_a_term_to_itself_becomes_two_times_the_term() {
            assertThat(simplify(add(X, X))).isEqualTo(mul(num(2), X));
        }

        @Test
        void works_for_compound_terms_too() {
            Expr term = add(X, Y);
            assertThat(simplify(add(term, term))).isEqualTo(mul(num(2), term));
        }

        @Test
        void different_terms_are_left_alone() {
            assertThat(simplify(add(X, Y))).isEqualTo(add(X, Y));
        }
    }

    @Nested
    class ConstantGrouping {

        @Test
        void a_constant_times_a_constant_times_a_term_is_grouped() {
            assertThat(simplify(mul(num(2), mul(num(3), X)))).isEqualTo(mul(num(6), X));
        }

        @Test
        void a_constant_on_the_right_is_not_moved_to_the_left() {
            // No commutative reordering: the simplifier only knows the rules listed in the task.
            assertThat(simplify(mul(num(2), mul(X, num(3))))).isEqualTo(mul(num(2), mul(X, num(3))));
        }
    }

    @Nested
    class CombinedRules {

        @Test
        void an_inner_simplification_enables_an_outer_one() {
            Expr formula = mul(add(mul(X, num(1)), num(0)), add(pow(Y, 1), num(0)));
            assertThat(simplify(formula)).isEqualTo(mul(X, Y));
        }

        @Test
        void negations_and_zeros_cancel_out_in_one_go() {
            assertThat(simplify(neg(neg(add(X, num(0)))))).isEqualTo(X);
        }
    }

    @Nested
    class FixedPoint {

        @Test
        void a_rule_output_that_matches_another_rule_is_simplified_again() {
            // 2x + 2x -> 2 * (2x) -> 4x. The second step only exists because of the fixed-point loop.
            Expr twiceTwoX = add(mul(num(2), X), mul(num(2), X));
            assertThat(simplify(twiceTwoX)).isEqualTo(mul(num(4), X));
        }
    }

    @Nested
    class Leftovers {

        @ParameterizedTest(name = "{0} stays as it is")
        @MethodSource("irreducible")
        void formulas_without_a_rule_stay_the_same(Expr expr) {
            assertThat(simplify(expr)).isEqualTo(expr);
        }

        static Stream<Expr> irreducible() {
            return Stream.of(X, add(X, Y), mul(X, Y), mul(X, num(2)), neg(X), pow(X, 2), neg(pow(X, 2)));
        }
    }

    /** Ten formulas that exercise several rules at once. Every variable used here has a value in ASSIGNMENTS. */
    static Stream<Expr> samples() {
        Expr x = X;
        Expr y = Y;
        Expr z = var("z");
        return Stream.of(
                mul(add(mul(x, num(1)), num(0)), add(pow(y, 1), num(0))),
                mul(add(x, x), mul(num(2), mul(num(3), y))),
                neg(neg(add(x, num(0)))),
                add(pow(y, 1), mul(num(0), z)),
                mul(pow(add(x, num(1)), 2), num(1)),
                add(mul(num(2), x), mul(num(2), x)),
                mul(num(3), mul(num(2), add(x, y))),
                neg(mul(num(1), pow(z, 0))),
                add(add(x, num(0)), add(y, add(z, num(0)))),
                pow(neg(pow(x, 1)), 3));
    }

    /** Three different assignments, chosen so that signs and zero both show up. */
    private static final List<Map<String, Long>> ASSIGNMENTS = List.of(
            Map.of("x", 3L, "y", -2L, "z", 5L),
            Map.of("x", 0L, "y", 7L, "z", -1L),
            Map.of("x", -4L, "y", 1L, "z", 2L));

    @ParameterizedTest
    @MethodSource("samples")
    void simplifying_keeps_the_value_for_every_assignment(Expr expr) {
        Expr simplified = simplify(expr);
        for (Map<String, Long> variables : ASSIGNMENTS) {
            assertThat(Evaluator.evaluate(simplified, variables)).isEqualTo(Evaluator.evaluate(expr, variables));
        }
    }

    @ParameterizedTest
    @MethodSource("samples")
    void simplifying_twice_changes_nothing(Expr expr) {
        Expr once = simplify(expr);
        assertThat(simplify(once)).isEqualTo(once);
    }
}
