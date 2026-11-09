package io.github.chris013.practice.task25.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class BillingPeriodTest {

    private static final LocalDate APRIL_1 = LocalDate.of(2026, 4, 1);
    private static final LocalDate MAY_1 = LocalDate.of(2026, 5, 1);
    private static final LocalDate JUNE_1 = LocalDate.of(2026, 6, 1);

    @Nested
    class Months {

        @ParameterizedTest
        @CsvSource({"2026-01-01, 31", "2026-02-01, 28", "2028-02-01, 29", "2026-04-01, 30", "2026-12-01, 31"})
        void a_monthly_period_has_the_length_of_its_calendar_month(String start, int expectedDays) {
            assertThat(BillingPeriod.monthStartingOn(LocalDate.parse(start)).lengthInDays()).isEqualTo(expectedDays);
        }

        @Test
        void a_period_starting_on_january_31st_ends_on_february_28th() {
            BillingPeriod period = BillingPeriod.monthStartingOn(LocalDate.of(2026, 1, 31));

            assertThat(period.endExclusive()).isEqualTo(LocalDate.of(2026, 2, 28));
            assertThat(period.lengthInDays()).isEqualTo(28);
        }

        @Test
        void the_next_period_starts_on_the_day_the_previous_one_ends() {
            assertThat(BillingPeriod.monthStartingOn(APRIL_1).next())
                    .isEqualTo(new BillingPeriod(MAY_1, JUNE_1));
        }
    }

    @Nested
    class DaysRemaining {

        @ParameterizedTest
        @CsvSource({"2026-04-01, 30", "2026-04-14, 17", "2026-04-30, 1"})
        void count_from_the_given_day_inclusive_up_to_the_end(String day, int expectedDays) {
            BillingPeriod april = BillingPeriod.monthStartingOn(APRIL_1);

            assertThat(april.daysRemainingFrom(LocalDate.parse(day))).isEqualTo(expectedDays);
        }

        @ParameterizedTest
        @ValueSource(strings = {"2026-03-31", "2026-05-01"})
        void days_outside_the_period_are_rejected(String day) {
            BillingPeriod april = BillingPeriod.monthStartingOn(APRIL_1);

            assertThatThrownBy(() -> april.daysRemainingFrom(LocalDate.parse(day)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Validation {

        @Test
        void the_end_must_be_after_the_start() {
            assertThatThrownBy(() -> new BillingPeriod(APRIL_1, APRIL_1))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BillingPeriod(MAY_1, APRIL_1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_null_dates() {
            assertThatNullPointerException().isThrownBy(() -> new BillingPeriod(null, MAY_1));
            assertThatNullPointerException().isThrownBy(() -> new BillingPeriod(APRIL_1, null));
        }
    }
}
