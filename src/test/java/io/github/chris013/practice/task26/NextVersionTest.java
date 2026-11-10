package io.github.chris013.practice.task26;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NextVersionTest {

    @Nested
    class NextPatch {

        @ParameterizedTest
        @CsvSource({
            "0.0.0, 0.0.1",
            "1.2.3, 1.2.4",
            "1.2.3+build.5, 1.2.4",
            "1.2.3-rc.1, 1.2.3",
            "1.2.3-rc.1+b, 1.2.3",
        })
        void drops_the_pre_release_or_bumps_the_patch(String from, String expected) {
            assertThat(SemanticVersion.parse(from).nextPatch()).hasToString(expected);
        }
    }

    @Nested
    class NextMinor {

        @ParameterizedTest
        @CsvSource({
            "1.0.0, 1.1.0",
            "1.2.3, 1.3.0",
            "1.2.3+b, 1.3.0",
            "1.2.3-rc.1, 1.3.0",
            "1.3.0-rc.1, 1.3.0",
        })
        void bumps_the_minor_or_drops_a_pre_release_on_a_zero_patch(String from, String expected) {
            assertThat(SemanticVersion.parse(from).nextMinor()).hasToString(expected);
        }
    }

    @Nested
    class NextMajor {

        @ParameterizedTest
        @CsvSource({
            "1.2.3, 2.0.0",
            "1.2.3+b, 2.0.0",
            "2.0.0-rc.1, 2.0.0",
            "2.1.0-rc.1, 3.0.0",
            "2.0.1-rc.1, 3.0.0",
        })
        void bumps_the_major_or_drops_a_pre_release_on_zero_minor_and_patch(String from, String expected) {
            assertThat(SemanticVersion.parse(from).nextMajor()).hasToString(expected);
        }
    }

    @Nested
    class Overflow {

        @Test
        void refuses_to_wrap_around_past_the_largest_int() {
            SemanticVersion largest = SemanticVersion.of(Integer.MAX_VALUE, 0, 0);

            assertThatThrownBy(largest::nextMajor).isInstanceOf(ArithmeticException.class);
        }
    }
}
