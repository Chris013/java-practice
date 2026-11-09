package io.github.chris013.practice.task19;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Working days for a German delivery company: Monday to Friday, minus public holidays.
 *
 * <p>Task 19: read {@code tasks/19-tdd-working-days.md} first.
 */
public final class WorkingDayCalendar {

    private WorkingDayCalendar(List<Holiday> holidays) {
        throw new UnsupportedOperationException("TODO: Task 19");
    }

    /** A calendar with the nationwide German holidays only. */
    public static WorkingDayCalendar germany() {
        throw new UnsupportedOperationException("TODO: Task 19");
    }

    /** The nationwide holidays plus the holidays of the given state. */
    public static WorkingDayCalendar germany(GermanState state) {
        throw new UnsupportedOperationException("TODO: Task 19");
    }

    /** True if the date is a Monday to Friday and not a holiday in this calendar. */
    public boolean isWorkingDay(LocalDate date) {
        throw new UnsupportedOperationException("TODO: Task 19");
    }

    /** The name of the holiday on this date, or empty. A holiday on a Saturday or Sunday still has a name. */
    public Optional<String> holidayOn(LocalDate date) {
        throw new UnsupportedOperationException("TODO: Task 19");
    }

    /**
     * The date reached after {@code days} working days. Working days are counted after {@code start}.
     * With {@code days == 0} the result is {@code start} if it is a working day, otherwise the next working day.
     *
     * @throws IllegalArgumentException if {@code days} is negative
     */
    public LocalDate addWorkingDays(LocalDate start, int days) {
        throw new UnsupportedOperationException("TODO: Task 19");
    }

    /**
     * The number of working days in {@code [startInclusive, endExclusive)}.
     *
     * @throws IllegalArgumentException if {@code endExclusive} is before {@code startInclusive}
     */
    public long workingDaysBetween(LocalDate startInclusive, LocalDate endExclusive) {
        throw new UnsupportedOperationException("TODO: Task 19");
    }

    /** All holiday dates in the year, in date order, including those that fall on a weekend. */
    public List<LocalDate> holidaysIn(int year) {
        throw new UnsupportedOperationException("TODO: Task 19");
    }
}
