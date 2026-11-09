package io.github.chris013.practice.task25.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Given. A half-open range of days, {@code start} inclusive and {@code endExclusive} not included.
 *
 * <p>Half-open ranges make adjacent periods line up without gaps or overlaps: January ends on the 1st of
 * February, and that same day is the first day of the next period.
 */
public record BillingPeriod(LocalDate start, LocalDate endExclusive) {

    public BillingPeriod {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(endExclusive, "endExclusive");
        if (!endExclusive.isAfter(start)) {
            throw new IllegalArgumentException("A period must end after it starts: %s to %s".formatted(start, endExclusive));
        }
    }

    /**
     * One calendar month. {@link LocalDate#plusMonths} clamps the day of the month: January 31st plus one month is
     * February 28th (29th in a leap year). That is the sensible answer to "one month later", but the anchor day
     * is lost. A subscription that started on the 31st keeps billing on the 28th from then on.
     */
    public static BillingPeriod monthStartingOn(LocalDate start) {
        Objects.requireNonNull(start, "start");
        return new BillingPeriod(start, start.plusMonths(1));
    }

    public int lengthInDays() {
        return (int) ChronoUnit.DAYS.between(start, endExclusive);
    }

    /** Days from {@code day} (inclusive) up to the end of the period. The day must be inside the period. */
    public int daysRemainingFrom(LocalDate day) {
        Objects.requireNonNull(day, "day");
        if (day.isBefore(start) || !day.isBefore(endExclusive)) {
            throw new IllegalArgumentException("%s is outside the period %s to %s".formatted(day, start, endExclusive));
        }
        return (int) ChronoUnit.DAYS.between(day, endExclusive);
    }

    /** The period that directly follows this one. */
    public BillingPeriod next() {
        return monthStartingOn(endExclusive);
    }
}
