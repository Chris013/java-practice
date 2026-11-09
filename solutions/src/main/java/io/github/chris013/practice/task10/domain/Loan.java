package io.github.chris013.practice.task10.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * One copy on loan to one member. Immutable: renewing returns a new loan instead of changing this one.
 *
 * <p>The fee and renewal rules live here, next to the data they depend on. They need no database, no clock and no
 * service: just the dates of this loan. That is what makes them trivial to test.
 */
public record Loan(CopyId copyId, MemberId memberId, LocalDate borrowedOn, LocalDate dueOn, int renewals) {

    static final int LOAN_PERIOD_DAYS = 21;
    static final int MAX_RENEWALS = 1;
    static final BigDecimal FEE_PER_LATE_DAY = new BigDecimal("0.50");
    static final BigDecimal MAX_LATE_FEE = new BigDecimal("10.00");
    private static final BigDecimal NO_FEE = new BigDecimal("0.00");

    public Loan {
        Objects.requireNonNull(copyId, "copyId");
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(borrowedOn, "borrowedOn");
        Objects.requireNonNull(dueOn, "dueOn");
        if (renewals < 0) {
            throw new IllegalArgumentException("renewals must not be negative, was " + renewals);
        }
    }

    /**
     * A new loan starting on {@code today}. The date is passed in rather than read from a clock, so the domain
     * never needs to know what time it is.
     */
    public static Loan start(CopyId copyId, MemberId memberId, LocalDate today) {
        Objects.requireNonNull(today, "today");
        return new Loan(copyId, memberId, today, today.plusDays(LOAN_PERIOD_DAYS), 0);
    }

    /** True only on days after the due date. On the due date itself the loan is still fine. */
    public boolean isOverdueOn(LocalDate day) {
        Objects.requireNonNull(day, "day");
        return day.isAfter(dueOn);
    }

    /**
     * One fee per calendar day after the due date: a return one day late costs EUR 0.50. The cap stops a forgotten
     * book from costing the member more than it is worth.
     */
    public BigDecimal lateFeeOn(LocalDate returnedOn) {
        Objects.requireNonNull(returnedOn, "returnedOn");
        if (!returnedOn.isAfter(dueOn)) {
            return NO_FEE;
        }
        long lateDays = ChronoUnit.DAYS.between(dueOn, returnedOn);
        BigDecimal fee = FEE_PER_LATE_DAY.multiply(BigDecimal.valueOf(lateDays));
        return fee.min(MAX_LATE_FEE).setScale(2, RoundingMode.UNNECESSARY);
    }

    /**
     * Renewing is refused by throwing rather than by returning a flag: a caller that forgets to check the flag
     * would silently extend a loan that must not be extended.
     */
    public Loan renewedOn(LocalDate today) {
        Objects.requireNonNull(today, "today");
        if (renewals >= MAX_RENEWALS) {
            throw new IllegalStateException("copy " + copyId.value() + " was already renewed");
        }
        if (isOverdueOn(today)) {
            throw new IllegalStateException("copy " + copyId.value() + " is overdue and cannot be renewed");
        }
        return new Loan(copyId, memberId, borrowedOn, dueOn.plusDays(LOAN_PERIOD_DAYS), renewals + 1);
    }
}
