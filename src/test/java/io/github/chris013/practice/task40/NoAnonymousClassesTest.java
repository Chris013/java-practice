package io.github.chris013.practice.task40;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Checks the refactoring result by name. Anonymous classes compile to {@code Outer$1}, {@code Outer$2}, ...;
 * lambdas and method references do not create class files. So if the classes are gone, the code uses lambdas.
 */
class NoAnonymousClassesTest {

    private static final String PACKAGE = "io.github.chris013.practice.task40.";

    @Test
    void coupon_service_has_no_anonymous_classes() {
        // The legacy file has 11 anonymous classes; checking up to 20 leaves room for any numbering.
        List<String> found = IntStream.rangeClosed(1, 20)
                .mapToObj(n -> "CouponService$" + n)
                .filter(NoAnonymousClassesTest::exists)
                .toList();

        assertThat(found).as("anonymous classes still compiled from CouponService").isEmpty();
    }

    @Test
    void no_chain_or_template_classes_remain() {
        List<String> found = Stream.of("ProcessingStep", "AbstractDiscountCalculator", "ValidationRule")
                .filter(NoAnonymousClassesTest::exists)
                .toList();

        assertThat(found).as("classes that the lambda version replaces").isEmpty();
    }

    private static boolean exists(String simpleName) {
        try {
            Class.forName(PACKAGE + simpleName, false, NoAnonymousClassesTest.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException notThere) {
            return false;
        }
    }
}
