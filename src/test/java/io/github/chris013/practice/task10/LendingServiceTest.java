package io.github.chris013.practice.task10;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.chris013.practice.task10.adapter.inmemory.InMemoryCatalog;
import io.github.chris013.practice.task10.adapter.inmemory.InMemoryLoanRepository;
import io.github.chris013.practice.task10.adapter.inmemory.InMemoryMemberDirectory;
import io.github.chris013.practice.task10.application.LendingService;
import io.github.chris013.practice.task10.domain.CopyId;
import io.github.chris013.practice.task10.domain.LendingRefusedException;
import io.github.chris013.practice.task10.domain.Loan;
import io.github.chris013.practice.task10.domain.Member;
import io.github.chris013.practice.task10.domain.MemberId;
import io.github.chris013.practice.task10.domain.RefusalReason;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LendingServiceTest {

    private static final LocalDate MARCH_1 = LocalDate.of(2026, 3, 1);
    private static final LocalDate APRIL_12 = LocalDate.of(2026, 4, 12);

    private static final MemberId ANNA = new MemberId("anna");
    private static final MemberId BEN = new MemberId("ben");
    private static final MemberId CAROL = new MemberId("carol");
    private static final CopyId C1 = new CopyId("copy-1");
    private static final CopyId C2 = new CopyId("copy-2");
    private static final CopyId C3 = new CopyId("copy-3");
    private static final CopyId C4 = new CopyId("copy-4");
    private static final CopyId C5 = new CopyId("copy-5");
    private static final CopyId C6 = new CopyId("copy-6");
    private static final CopyId UNKNOWN = new CopyId("no-such-copy");

    private final MutableClock clock = new MutableClock(MARCH_1.atStartOfDay(ZoneOffset.UTC).toInstant());
    private final InMemoryLoanRepository loans = new InMemoryLoanRepository();
    private final InMemoryMemberDirectory members = new InMemoryMemberDirectory();
    private final InMemoryCatalog catalog = new InMemoryCatalog();
    private final LendingService lending = new LendingService(loans, members, catalog, clock);

    LendingServiceTest() {
        members.add(new Member(ANNA, false));
        members.add(new Member(BEN, true));
        members.add(new Member(CAROL, false));
        catalog.add(C1, C2, C3, C4, C5, C6);
    }

    /** Something that may throw; a lambda such as {@code () -> lending.borrow(ANNA, C1)}. */
    @FunctionalInterface
    private interface Action {
        void run() throws Throwable;
    }

    /** Runs the action and returns the refusal reason; fails the test if the action succeeds. */
    private static RefusalReason refusalOf(Action action) {
        try {
            action.run();
        } catch (LendingRefusedException refusal) {
            return refusal.reason();
        } catch (Throwable other) {
            throw new AssertionError("expected a refusal but got " + other, other);
        }
        throw new AssertionError("expected a refusal but the call succeeded");
    }

    private void advanceDays(long days) {
        clock.advanceBy(Duration.ofDays(days));
    }

    @Nested
    class Construction {

        @Test
        void rejects_missing_collaborators() {
            assertThatThrownBy(() -> new LendingService(null, members, catalog, clock))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new LendingService(loans, null, catalog, clock))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new LendingService(loans, members, null, clock))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new LendingService(loans, members, catalog, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void takes_today_from_the_injected_clock() {
            Clock fixed = Clock.fixed(MARCH_1.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
            LendingService service = new LendingService(loans, members, catalog, fixed);

            assertThat(service.borrow(ANNA, C1).borrowedOn()).isEqualTo(MARCH_1);
        }
    }

    @Nested
    class Borrowing {

        @Test
        void records_a_loan_due_three_weeks_later() {
            Loan loan = lending.borrow(ANNA, C1);

            assertThat(loan.borrowedOn()).isEqualTo(MARCH_1);
            assertThat(loan.dueOn()).isEqualTo(LocalDate.of(2026, 3, 22));
            assertThat(loans.findActiveLoanOf(C1)).contains(loan);
        }

        @Test
        void refuses_an_unknown_member() {
            assertThat(refusalOf(() -> lending.borrow(new MemberId("zoe"), C1)))
                    .isEqualTo(RefusalReason.UNKNOWN_MEMBER);
        }

        @Test
        void refuses_a_blocked_member() {
            assertThat(refusalOf(() -> lending.borrow(BEN, C1))).isEqualTo(RefusalReason.MEMBER_BLOCKED);
        }

        @Test
        void refuses_while_the_member_has_an_overdue_loan() {
            lending.borrow(ANNA, C1);
            advanceDays(22); // due on 22 March, today is 23 March

            assertThat(refusalOf(() -> lending.borrow(ANNA, C2))).isEqualTo(RefusalReason.HAS_OVERDUE_LOANS);
        }

        @Test
        void allows_borrowing_on_the_due_day_of_another_loan() {
            lending.borrow(ANNA, C1);
            advanceDays(21); // 22 March, the due day itself is not overdue

            assertThat(lending.borrow(ANNA, C2).copyId()).isEqualTo(C2);
        }

        @Test
        void allows_five_loans_but_refuses_a_sixth() {
            for (CopyId copy : List.of(C1, C2, C3, C4, C5)) {
                lending.borrow(ANNA, copy);
            }

            assertThat(refusalOf(() -> lending.borrow(ANNA, C6))).isEqualTo(RefusalReason.LOAN_LIMIT_REACHED);
        }

        @Test
        void refuses_an_unknown_copy_and_saves_nothing() {
            assertThat(refusalOf(() -> lending.borrow(ANNA, UNKNOWN))).isEqualTo(RefusalReason.UNKNOWN_COPY);
            assertThat(loans.findActiveLoansOf(ANNA)).isEmpty();
        }

        @Test
        void refuses_a_copy_that_is_already_on_loan() {
            lending.borrow(ANNA, C1);

            assertThat(refusalOf(() -> lending.borrow(CAROL, C1))).isEqualTo(RefusalReason.COPY_ALREADY_ON_LOAN);
            assertThat(loans.findActiveLoanOf(C1).orElseThrow().memberId()).isEqualTo(ANNA);
        }

        @Test
        void checks_the_member_before_the_copy() {
            assertThat(refusalOf(() -> lending.borrow(new MemberId("zoe"), UNKNOWN)))
                    .isEqualTo(RefusalReason.UNKNOWN_MEMBER);
        }

        @Test
        void reports_a_blocked_member_before_overdue_loans() {
            lending.borrow(ANNA, C1);
            advanceDays(22);
            members.add(new Member(ANNA, true));

            assertThat(refusalOf(() -> lending.borrow(ANNA, C2))).isEqualTo(RefusalReason.MEMBER_BLOCKED);
        }

        @Test
        void reports_overdue_loans_before_the_loan_limit() {
            for (CopyId copy : List.of(C1, C2, C3, C4, C5)) {
                lending.borrow(ANNA, copy);
            }
            advanceDays(22);

            assertThat(refusalOf(() -> lending.borrow(ANNA, C6))).isEqualTo(RefusalReason.HAS_OVERDUE_LOANS);
        }

        @Test
        void reports_the_loan_limit_before_an_unknown_copy() {
            for (CopyId copy : List.of(C1, C2, C3, C4, C5)) {
                lending.borrow(ANNA, copy);
            }

            assertThat(refusalOf(() -> lending.borrow(ANNA, UNKNOWN))).isEqualTo(RefusalReason.LOAN_LIMIT_REACHED);
        }
    }

    @Nested
    class GivingBack {

        @Test
        void refuses_a_copy_that_is_not_on_loan() {
            assertThat(refusalOf(() -> lending.giveBack(C1))).isEqualTo(RefusalReason.COPY_NOT_ON_LOAN);
        }

        @Test
        void closes_the_loan_without_a_fee_when_returned_on_time() {
            lending.borrow(ANNA, C1);
            advanceDays(21); // returned on the due day

            var receipt = lending.giveBack(C1);

            assertThat(receipt.lateFee()).isEqualTo(new BigDecimal("0.00"));
            assertThat(receipt.returnedOn()).isEqualTo(LocalDate.of(2026, 3, 22));
            assertThat(loans.findActiveLoanOf(C1)).isEmpty();
        }

        @Test
        void charges_fifty_cents_per_day_late() {
            lending.borrow(ANNA, C1);
            advanceDays(24); // three days after the due date

            assertThat(lending.giveBack(C1).lateFee()).isEqualTo(new BigDecimal("1.50"));
        }

        @Test
        void refuses_a_second_return_of_the_same_copy() {
            lending.borrow(ANNA, C1);
            lending.giveBack(C1);

            assertThat(refusalOf(() -> lending.giveBack(C1))).isEqualTo(RefusalReason.COPY_NOT_ON_LOAN);
        }

        @Test
        void frees_the_copy_for_the_next_member() {
            lending.borrow(ANNA, C1);
            lending.giveBack(C1);

            assertThat(lending.borrow(CAROL, C1).memberId()).isEqualTo(CAROL);
        }
    }

    @Nested
    class Renewing {

        @Test
        void refuses_a_copy_that_is_not_on_loan() {
            assertThat(refusalOf(() -> lending.renew(C1))).isEqualTo(RefusalReason.COPY_NOT_ON_LOAN);
        }

        @Test
        void extends_the_due_date_and_saves_the_renewed_loan() {
            lending.borrow(ANNA, C1);
            advanceDays(5);

            Loan renewed = lending.renew(C1);

            assertThat(renewed.dueOn()).isEqualTo(APRIL_12);
            assertThat(renewed.renewals()).isEqualTo(1);
            assertThat(loans.findActiveLoanOf(C1)).contains(renewed);
        }

        @Test
        void allows_a_renewal_on_the_due_day() {
            lending.borrow(ANNA, C1);
            advanceDays(21);

            assertThat(lending.renew(C1).renewals()).isEqualTo(1);
        }

        @Test
        void refuses_a_second_renewal() {
            lending.borrow(ANNA, C1);
            lending.renew(C1);

            assertThat(refusalOf(() -> lending.renew(C1))).isEqualTo(RefusalReason.RENEWAL_NOT_ALLOWED);
        }

        @Test
        void refuses_to_renew_an_overdue_loan() {
            lending.borrow(ANNA, C1);
            advanceDays(22);

            assertThat(refusalOf(() -> lending.renew(C1))).isEqualTo(RefusalReason.RENEWAL_NOT_ALLOWED);
        }
    }
}
