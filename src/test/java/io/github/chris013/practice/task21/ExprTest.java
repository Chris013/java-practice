package io.github.chris013.practice.task21;

import static io.github.chris013.practice.task21.Expr.add;
import static io.github.chris013.practice.task21.Expr.neg;
import static io.github.chris013.practice.task21.Expr.num;
import static io.github.chris013.practice.task21.Expr.pow;
import static io.github.chris013.practice.task21.Expr.var;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExprTest {

    @Nested
    class Variables {

        @ParameterizedTest
        @ValueSource(strings = {"x", "total", "vatRate", "x2", "p9Q"})
        void accept_lower_camel_case_names(String name) {
            assertThatCode(() -> var(name)).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "X", "2x", "x_y", "x y", "x-y"})
        void reject_other_names(String name) {
            assertThatThrownBy(() -> var(name))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(name);
        }

        @Test
        void reject_a_null_name() {
            assertThatThrownBy(() -> var(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Powers {

        @Test
        void accept_an_exponent_of_zero() {
            assertThatCode(() -> pow(var("x"), 0)).doesNotThrowAnyException();
        }

        @Test
        void reject_a_negative_exponent() {
            assertThatThrownBy(() -> pow(var("x"), -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("-1");
        }
    }

    @Nested
    class Components {

        @Test
        void must_not_be_null() {
            assertThatThrownBy(() -> add(null, num(1))).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> add(num(1), null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> neg(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> pow(null, 2)).isInstanceOf(NullPointerException.class);
        }
    }
}
