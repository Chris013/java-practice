package io.github.chris013.practice.task21;

import static io.github.chris013.practice.task21.Expr.add;
import static io.github.chris013.practice.task21.Expr.mul;
import static io.github.chris013.practice.task21.Expr.neg;
import static io.github.chris013.practice.task21.Expr.num;
import static io.github.chris013.practice.task21.Expr.pow;
import static io.github.chris013.practice.task21.Expr.var;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EvaluatorTest {

    private static final Map<String, Long> VARIABLES = Map.of("x", 3L, "y", -2L);

    private static long eval(Expr expr) {
        return Evaluator.evaluate(expr, VARIABLES);
    }

    @Nested
    class Leaves {

        @Test
        void a_number_is_its_own_value() {
            assertThat(eval(num(42))).isEqualTo(42);
        }

        @Test
        void a_variable_is_looked_up_by_name() {
            assertThat(eval(var("y"))).isEqualTo(-2);
        }

        @Test
        void an_unknown_variable_is_rejected_and_named() {
            assertThatThrownBy(() -> eval(var("z")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("z");
        }
    }

    @Nested
    class Operations {

        @Test
        void add_and_multiply_follow_the_tree() {
            assertThat(eval(add(num(2), mul(num(3), num(4))))).isEqualTo(14);
            assertThat(eval(mul(add(num(2), num(3)), num(4)))).isEqualTo(20);
        }

        @Test
        void negation_flips_the_sign() {
            assertThat(eval(neg(var("x")))).isEqualTo(-3);
            assertThat(eval(neg(neg(var("x"))))).isEqualTo(3);
        }

        @ParameterizedTest(name = "{0}^{1} = {2}")
        @CsvSource({"3, 0, 1", "0, 0, 1", "3, 1, 3", "3, 4, 81", "-2, 3, -8", "2, 62, 4611686018427387904",
            "-2, 63, -9223372036854775808"})
        void powers_are_exact(long base, int exponent, long expected) {
            assertThat(eval(pow(num(base), exponent))).isEqualTo(expected);
        }

        @Test
        void a_power_of_minus_one_or_one_with_a_huge_exponent_is_fast() {
            assertThat(eval(pow(num(-1), 1_000_001))).isEqualTo(-1);
            assertThat(eval(pow(num(1), 1_000_000_000))).isEqualTo(1);
        }

        @Test
        void nested_expressions_use_the_variables() {
            assertThat(eval(pow(add(var("x"), num(1)), 2))).isEqualTo(16);
        }
    }

    @Nested
    class Overflow {

        @Test
        void addition_that_does_not_fit_throws() {
            assertThatThrownBy(() -> eval(add(num(Long.MAX_VALUE), num(1))))
                    .isInstanceOf(ArithmeticException.class);
        }

        @Test
        void multiplication_that_does_not_fit_throws() {
            assertThatThrownBy(() -> eval(mul(num(Long.MAX_VALUE), num(2))))
                    .isInstanceOf(ArithmeticException.class);
        }

        @Test
        void power_that_does_not_fit_throws() {
            assertThatThrownBy(() -> eval(pow(num(2), 63))).isInstanceOf(ArithmeticException.class);
        }
    }
}
