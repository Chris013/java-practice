package io.github.chris013.practice.task01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyTest {

    private static final Currency EUR = Currency.getInstance("EUR");

    @Nested
    class Creation {

        @Test
        void normalizes_the_amount_to_the_minor_unit_of_the_currency() {
            assertThat(Money.of("10", "EUR").amount()).isEqualTo(new BigDecimal("10.00"));
            assertThat(Money.of("10.5", "EUR").amount()).isEqualTo(new BigDecimal("10.50"));
        }

        @Test
        void knows_that_yen_has_no_minor_unit() {
            assertThat(Money.of("500", "JPY").amount()).isEqualTo(new BigDecimal("500"));
        }

        @Test
        void refuses_amounts_more_precise_than_the_currency_allows() {
            assertThatThrownBy(() -> Money.of("1.005", "EUR")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Money.of("1.5", "JPY")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void refuses_null() {
            assertThatThrownBy(() -> Money.of(null, EUR)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Money.of(BigDecimal.ONE, null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void zero_is_zero_in_the_given_currency() {
            Money zero = Money.zero(EUR);

            assertThat(zero.isZero()).isTrue();
            assertThat(zero.currency()).isEqualTo(EUR);
            assertThat(zero).isEqualTo(Money.of("0.00", "EUR"));
        }
    }

    @Nested
    class Equality {

        @Test
        void is_based_on_value_not_on_notation() {
            Money a = Money.of("10", "EUR");
            Money b = Money.of(new BigDecimal("10.00"), EUR);

            assertThat(a).isEqualTo(b);
            assertThat(a).hasSameHashCodeAs(b);
        }

        @Test
        void takes_the_currency_into_account() {
            assertThat(Money.of("10", "EUR")).isNotEqualTo(Money.of("10", "USD"));
        }

        @Test
        void prints_currency_code_and_amount() {
            assertThat(Money.of("19.9", "EUR")).hasToString("EUR 19.90");
            assertThat(Money.of("500", "JPY")).hasToString("JPY 500");
            assertThat(Money.of("-3", "USD")).hasToString("USD -3.00");
        }
    }

    @Nested
    class Arithmetic {

        @Test
        void adds_and_subtracts() {
            Money ten = Money.of("10.00", "EUR");
            Money three = Money.of("3.50", "EUR");

            assertThat(ten.plus(three)).isEqualTo(Money.of("13.50", "EUR"));
            assertThat(ten.minus(three)).isEqualTo(Money.of("6.50", "EUR"));
        }

        @Test
        void may_become_negative_because_refunds_exist() {
            Money result = Money.of("1.00", "EUR").minus(Money.of("2.50", "EUR"));

            assertThat(result).isEqualTo(Money.of("-1.50", "EUR"));
            assertThat(result.isNegative()).isTrue();
        }

        @Test
        void never_mutates_the_operands() {
            Money ten = Money.of("10.00", "EUR");

            ten.plus(Money.of("5.00", "EUR"));

            assertThat(ten).isEqualTo(Money.of("10.00", "EUR"));
        }

        @Test
        void multiplies_by_a_quantity() {
            assertThat(Money.of("2.99", "EUR").times(3)).isEqualTo(Money.of("8.97", "EUR"));
        }

        @Test
        void multiplies_by_a_factor_with_explicit_rounding() {
            Money price = Money.of("9.99", "EUR");

            assertThat(price.multipliedBy(new BigDecimal("0.19"), RoundingMode.HALF_UP))
                    .isEqualTo(Money.of("1.90", "EUR"));
        }

        @Test
        void uses_exactly_the_rounding_mode_it_is_given() {
            Money quarter = Money.of("0.25", "EUR");
            BigDecimal half = new BigDecimal("0.5");

            assertThat(quarter.multipliedBy(half, RoundingMode.HALF_UP)).isEqualTo(Money.of("0.13", "EUR"));
            assertThat(quarter.multipliedBy(half, RoundingMode.HALF_EVEN)).isEqualTo(Money.of("0.12", "EUR"));
        }

        @Test
        void refuses_to_combine_different_currencies() {
            Money euros = Money.of("10", "EUR");
            Money dollars = Money.of("10", "USD");

            assertThatThrownBy(() -> euros.plus(dollars))
                    .isInstanceOf(CurrencyMismatchException.class)
                    .hasMessageContaining("EUR")
                    .hasMessageContaining("USD");
            assertThatThrownBy(() -> euros.minus(dollars)).isInstanceOf(CurrencyMismatchException.class);
        }
    }

    @Nested
    class Comparison {

        @Test
        void compares_amounts_of_the_same_currency() {
            assertThat(Money.of("10", "EUR").isGreaterThan(Money.of("9.99", "EUR"))).isTrue();
            assertThat(Money.of("10", "EUR").isGreaterThan(Money.of("10", "EUR"))).isFalse();
        }

        @Test
        void refuses_to_compare_different_currencies() {
            assertThatThrownBy(() -> Money.of("10", "EUR").isGreaterThan(Money.of("1", "USD")))
                    .isInstanceOf(CurrencyMismatchException.class);
        }
    }

    @Nested
    class Allocation {

        @Test
        void splits_evenly_when_possible() {
            assertThat(Money.of("90.00", "EUR").allocate(1, 1, 1))
                    .containsExactly(Money.of("30", "EUR"), Money.of("30", "EUR"), Money.of("30", "EUR"));
        }

        @Test
        void never_loses_a_cent() {
            assertThat(Money.of("100.00", "EUR").allocate(1, 1, 1))
                    .containsExactly(Money.of("33.34", "EUR"), Money.of("33.33", "EUR"), Money.of("33.33", "EUR"));
        }

        @Test
        void respects_ratios() {
            assertThat(Money.of("0.05", "EUR").allocate(3, 7))
                    .containsExactly(Money.of("0.02", "EUR"), Money.of("0.03", "EUR"));
        }

        @Test
        void works_in_currencies_without_minor_unit() {
            assertThat(Money.of("1000", "JPY").allocate(1, 1, 1))
                    .containsExactly(Money.of("334", "JPY"), Money.of("333", "JPY"), Money.of("333", "JPY"));
        }

        @Test
        void never_gives_anything_to_a_party_with_ratio_zero() {
            assertThat(Money.of("0.01", "EUR").allocate(0, 1, 1))
                    .containsExactly(Money.of("0.00", "EUR"), Money.of("0.01", "EUR"), Money.of("0.00", "EUR"));
        }

        @ParameterizedTest
        @CsvSource({"100.00", "0.01", "0.00", "-10.00", "1234567.89"})
        void parts_always_add_up_to_the_original(String amount) {
            Money original = Money.of(amount, "EUR");

            List<Money> parts = original.allocate(3, 5, 0, 11, 1);

            Money sum = parts.stream().reduce(Money.zero(EUR), Money::plus);
            assertThat(sum).isEqualTo(original);
        }

        @Test
        void refuses_meaningless_ratios() {
            Money money = Money.of("10", "EUR");

            assertThatThrownBy(money::allocate).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> money.allocate(0, 0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> money.allocate(1, -1)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
