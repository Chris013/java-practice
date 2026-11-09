package io.github.chris013.practice.task10;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.chris013.practice.task10.domain.CopyId;
import io.github.chris013.practice.task10.domain.Loan;
import io.github.chris013.practice.task10.domain.MemberId;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LoanTest {

    private static final CopyId COPY = new CopyId("copy-1");
    private static final MemberId ANNA = new MemberId("anna");
    private static final LocalDate MARCH_1 = LocalDate.of(2026, 3, 1);
    /** 1 March + 21 days */
    private static final LocalDate MARCH_22 = LocalDate.of(2026, 3, 22);

    private final Loan loan = Loan.start(COPY, ANNA, MARCH_1);

    @Nested
    class Starting {

        @Test
        void is_due_three_weeks_after_today_with_no_renewals() {
            assertThat(loan.copyId()).isEqualTo(COPY);
            assertThat(loan.memberId()).isEqualTo(ANNA);
            assertThat(loan.borrowedOn()).isEqualTo(MARCH_1);
            assertThat(loan.dueOn()).isEqualTo(MARCH_22);
            assertThat(loan.renewals()).isZero();
        }
    }

    @Nested
    class Overdue {

        @ParameterizedTest
        @CsvSource({"-1, false", "0, false", "1, true", "30, true"})
        void starts_only_after_the_due_day(int dayOffset, boolean overdue) {
            assertThat(loan.isOverdueOn(MARCH_22.plusDays(dayOffset))).isEqualTo(overdue);
        }
    }

    @Nested
    class LateFee {

        @ParameterizedTest
        @CsvSource({
            "-3, 0.00",
            "0, 0.00",
            "1, 0.50",
            "2, 1.00",
            "3, 1.50",
            "19, 9.50",
            "20, 10.00",
            "21, 10.00",
            "90, 10.00"
        })
        void is_fifty_cents_per_day_late_capped_at_ten_euros(int daysLate, String fee) {
            // The fee is compared with equals, so the scale (2) is checked as well.
            assertThat(loan.lateFeeOn(MARCH_22.plusDays(daysLate))).isEqualTo(new BigDecimal(fee));
        }
    }

    @Nested
    class Renewing {

        @Test
        void moves_the_due_date_three_weeks_later_and_counts_the_renewal() {
            Loan renewed = loan.renewedOn(MARCH_1.plusDays(5));

            assertThat(renewed.dueOn()).isEqualTo(LocalDate.of(2026, 4, 12)); // 22 March + 21 days
            assertThat(renewed.renewals()).isEqualTo(1);
            assertThat(renewed.borrowedOn()).isEqualTo(MARCH_1);
        }

        @Test
        void is_allowed_on_the_due_day_itself() {
            assertThat(loan.renewedOn(MARCH_22).renewals()).isEqualTo(1);
        }

        @Test
        void is_allowed_only_once() {
            Loan renewed = loan.renewedOn(MARCH_1);

            assertThatThrownBy(() -> renewed.renewedOn(MARCH_1)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void is_refused_when_the_loan_is_overdue() {
            assertThatThrownBy(() -> loan.renewedOn(MARCH_22.plusDays(1)))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
