package io.github.chris013.practice.task21;

import static io.github.chris013.practice.task21.Expr.add;
import static io.github.chris013.practice.task21.Expr.mul;
import static io.github.chris013.practice.task21.Expr.neg;
import static io.github.chris013.practice.task21.Expr.num;
import static io.github.chris013.practice.task21.Expr.pow;
import static io.github.chris013.practice.task21.Expr.var;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PrinterTest {

    static Stream<Arguments> expressions() {
        Expr x = var("x");
        Expr y = var("y");
        Expr a = var("a");
        Expr b = var("b");
        Expr c = var("c");
        return Stream.of(
                Arguments.of(num(7), "7"),
                Arguments.of(num(-3), "-3"),
                Arguments.of(x, "x"),
                Arguments.of(add(x, num(1)), "x + 1"),
                Arguments.of(mul(num(2), add(x, num(1))), "2 * (x + 1)"),
                Arguments.of(mul(num(2), x), "2 * x"),
                Arguments.of(add(mul(num(2), x), num(1)), "2 * x + 1"),
                Arguments.of(neg(x), "-x"),
                Arguments.of(neg(add(x, num(1))), "-(x + 1)"),
                Arguments.of(neg(mul(num(2), x)), "-(2 * x)"),
                Arguments.of(neg(neg(x)), "-(-x)"),
                Arguments.of(neg(num(-3)), "-(-3)"),
                Arguments.of(neg(pow(x, 2)), "-x^2"),
                Arguments.of(pow(x, 2), "x^2"),
                Arguments.of(pow(x, 0), "x^0"),
                Arguments.of(pow(add(x, num(1)), 2), "(x + 1)^2"),
                Arguments.of(pow(neg(x), 2), "(-x)^2"),
                Arguments.of(pow(num(-3), 2), "(-3)^2"),
                Arguments.of(pow(pow(x, 2), 3), "(x^2)^3"),
                Arguments.of(add(add(a, b), c), "a + b + c"),
                Arguments.of(add(a, add(b, c)), "a + (b + c)"),
                Arguments.of(mul(mul(a, b), c), "a * b * c"),
                Arguments.of(mul(a, mul(b, c)), "a * (b * c)"),
                Arguments.of(mul(add(a, b), c), "(a + b) * c"),
                Arguments.of(add(a, mul(b, c)), "a + b * c"),
                Arguments.of(add(a, neg(b)), "a + -b"),
                Arguments.of(mul(x, neg(y)), "x * -y"));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("expressions")
    void prints_with_only_the_parentheses_that_are_needed(Expr expr, String expected) {
        assertThat(Printer.print(expr)).isEqualTo(expected);
    }
}
