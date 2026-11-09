package io.github.chris013.practice.task10.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * One copy on loan to one member. Immutable: renewing returns a new loan instead of changing this one.
 *
 * <p>Task 10: read {@code tasks/10-library-lending-hexagonal.md} first.
 */
public record Loan(CopyId copyId, MemberId memberId, LocalDate borrowedOn, LocalDate dueOn, int renewals) {

    public Loan {
        Objects.requireNonNull(copyId, "copyId");
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(borrowedOn, "borrowedOn");
        Objects.requireNonNull(dueOn, "dueOn");
        if (renewals < 0) {
            throw new IllegalArgumentException("renewals must not be negative, was " + renewals);
        }
    }

    /** A new loan starting on {@code today}: due 21 days later, no renewals yet. */
    public static Loan start(CopyId copyId, MemberId memberId, LocalDate today) {
        throw new UnsupportedOperationException("TODO: Task 10");
    }

    /** True only on days after the due date. On the due date itself the loan is still fine. */
    public boolean isOverdueOn(LocalDate day) {
        throw new UnsupportedOperationException("TODO: Task 10");
    }

    /** EUR 0.50 per day after the due date, capped at EUR 10.00. EUR 0.00 when on time. Scale 2. */
    public BigDecimal lateFeeOn(LocalDate returnedOn) {
        throw new UnsupportedOperationException("TODO: Task 10");
    }

    /**
     * The same loan with 21 more days and one more renewal. Only once per loan, and never when overdue on
     * {@code today}; otherwise throws IllegalStateException.
     */
    public Loan renewedOn(LocalDate today) {
        throw new UnsupportedOperationException("TODO: Task 10");
    }
}
