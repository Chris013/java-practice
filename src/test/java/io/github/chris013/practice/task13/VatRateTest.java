package io.github.chris013.practice.task13;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class VatRateTest {

    @Nested
    class Values {

        @Test
        void the_standard_rate_is_code_A_at_19_percent() {
            assertThat(VatRate.STANDARD.code()).isEqualTo('A');
            assertThat(VatRate.STANDARD.percent()).isEqualTo(19);
        }

        @Test
        void the_reduced_rate_is_code_B_at_7_percent() {
            assertThat(VatRate.REDUCED.code()).isEqualTo('B');
            assertThat(VatRate.REDUCED.percent()).isEqualTo(7);
        }
    }

    @Nested
    class FromCode {

        @ParameterizedTest
        @CsvSource({"A, STANDARD", "B, REDUCED"})
        void finds_the_rate_for_a_printed_code(char code, VatRate expected) {
            assertThat(VatRate.fromCode(code)).isSameAs(expected);
        }

        @Test
        void round_trips_every_rate_through_its_code() {
            for (VatRate rate : VatRate.values()) {
                assertThat(VatRate.fromCode(rate.code())).isSameAs(rate);
            }
        }

        @Test
        void rejects_an_unknown_code_and_names_it() {
            assertThatThrownBy(() -> VatRate.fromCode('C'))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("'C'");
        }

        @Test
        void codes_are_case_sensitive_as_printed() {
            assertThatThrownBy(() -> VatRate.fromCode('a')).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
