package io.github.chris013.practice.task40;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Checks the refactoring result on the compiled classes. Anonymous classes compile to {@code Outer$1},
 * {@code Outer$2}, ...; lambdas and method references do not create class files. So if no anonymous class is
 * left, the code uses lambdas.
 */
class NoAnonymousClassesTest {

    private static final String PACKAGE = "io.github.chris013.practice.task40.";

    @Test
    void coupon_service_has_no_anonymous_classes() {
        // The legacy file has 12 anonymous classes. javac may also emit synthetic classes with the same
        // CouponService$N names (for example the lookup table of a switch on an enum); those are not ours.
        List<String> found = IntStream.rangeClosed(1, 20)
                .mapToObj(n -> load("CouponService$" + n))
                .flatMap(Optional::stream)
                .filter(c -> c.isAnonymousClass() && !c.isSynthetic())
                .map(Class::getName)
                .toList();

        assertThat(found).as("anonymous classes still compiled from CouponService").isEmpty();
    }

    @Test
    void no_chain_or_template_classes_remain() {
        List<String> found = Stream.of("ProcessingStep", "AbstractDiscountCalculator", "ValidationRule")
                .filter(name -> load(name).isPresent())
                .toList();

        assertThat(found).as("classes that the lambda version replaces").isEmpty();
    }

    private static Optional<Class<?>> load(String simpleName) {
        try {
            return Optional.of(Class.forName(PACKAGE + simpleName, false, NoAnonymousClassesTest.class.getClassLoader()));
        } catch (ClassNotFoundException notThere) {
            return Optional.empty();
        }
    }
}
