package io.github.chris013.practice.task15;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class GiftCardCodeTest {

    @Nested
    class Format {

        @Test
        void accepts_the_printed_format() {
            assertThat(new GiftCardCode("ABCD-1234-EFGH").value()).isEqualTo("ABCD-1234-EFGH");
        }

        @ParameterizedTest
        @CsvSource({
            "abcd-1234-efgh, ABCD-1234-EFGH",
            "a1b2-c3d4-e5f6, A1B2-C3D4-E5F6",
            "0000-ZZZZ-9999, 0000-ZZZZ-9999"
        })
        void upper_cases_the_code(String input, String expected) {
            assertThat(new GiftCardCode(input).value()).isEqualTo(expected);
        }

        @Test
        void trims_surrounding_whitespace_before_checking_the_format() {
            assertThat(new GiftCardCode(" abcd-1234-efgh ").value()).isEqualTo("ABCD-1234-EFGH");
        }

        @Test
        void equal_codes_after_normalisation_are_equal() {
            assertThat(new GiftCardCode("abcd-1234-efgh")).isEqualTo(new GiftCardCode("ABCD-1234-EFGH"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "",
            "   ",
            "ABCD1234EFGH",
            "ABCD-1234-EFG",
            "ABCD-1234-EFGHI",
            "ABCD-12-EFGH",
            "ABCD_1234_EFGH",
            "ABCD-1234-EFGé",
            "ABCD-1234-EFGH-"
        })
        void rejects_a_wrong_format_with_an_illegal_argument(String value) {
            assertThatThrownBy(() -> new GiftCardCode(value))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_null() {
            assertThatThrownBy(() -> new GiftCardCode(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
