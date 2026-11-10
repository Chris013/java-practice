package io.github.chris013.practice.task19;

import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Working days for a German delivery company: Monday to Friday, minus public holidays.
 *
 * <p>Holidays are data, not code: each calendar holds an immutable list of {@link Holiday} rules, so two calendars
 * never share state and the holiday rules can be read in one place. The holiday rules are fixed for the life of
 * the object, so every method is a pure function of its arguments.
 *
 * <p>The calendar supports dates from 1 January 1583, the first full year of the Gregorian calendar. Every method
 * throws {@code IllegalArgumentException} for an earlier date or year, because Easter, and with it several
 * holidays, is undefined there.
 */
public final class WorkingDayCalendar {

    private static final List<Holiday> NATIONAL = List.of(
            new FixedHoliday("New Year's Day", MonthDay.of(1, 1)),
            new EasterBasedHoliday("Good Friday", -2),
            new EasterBasedHoliday("Easter Monday", 1),
            new FixedHoliday("Labour Day", MonthDay.of(5, 1)),
            new EasterBasedHoliday("Ascension Day", 39),
            new EasterBasedHoliday("Whit Monday", 50),
            new FixedHoliday("German Unity Day", MonthDay.of(10, 3)),
            new FixedHoliday("Christmas Day", MonthDay.of(12, 25)),
            new FixedHoliday("Boxing Day", MonthDay.of(12, 26)));

    private final List<Holiday> holidays;

    private WorkingDayCalendar(List<Holiday> holidays) {
        this.holidays = List.copyOf(holidays);
    }

    /** A calendar with the nationwide German holidays only. */
    public static WorkingDayCalendar germany() {
        return new WorkingDayCalendar(NATIONAL);
    }

    /** The nationwide holidays plus the holidays of the given state. */
    public static WorkingDayCalendar germany(GermanState state) {
        Objects.requireNonNull(state, "state");
        List<Holiday> all = new ArrayList<>(NATIONAL);
        all.addAll(stateHolidays(state));
        return new WorkingDayCalendar(all);
    }

    /**
     * The switch has no default branch on purpose: if someone adds a state to {@link GermanState} and forgets
     * its holidays, the compiler stops the build instead of silently giving that state no state holidays.
     */
    private static List<Holiday> stateHolidays(GermanState state) {
        return switch (state) {
            case BAVARIA -> List.of(
                    new FixedHoliday("Epiphany", MonthDay.of(1, 6)),
                    new EasterBasedHoliday("Corpus Christi", 60),
                    new FixedHoliday("All Saints' Day", MonthDay.of(11, 1)),
                    new FixedHoliday("Assumption Day", MonthDay.of(8, 15)));
            case BERLIN -> List.of(
                    new FixedHoliday("International Women's Day", MonthDay.of(3, 8)));
            case NORTH_RHINE_WESTPHALIA -> List.of(
                    new EasterBasedHoliday("Corpus Christi", 60),
                    new FixedHoliday("All Saints' Day", MonthDay.of(11, 1)));
        };
    }

    private static void requireSupportedYear(int year) {
        if (year < Easter.FIRST_GREGORIAN_YEAR) {
            throw new IllegalArgumentException(
                    "Only years from " + Easter.FIRST_GREGORIAN_YEAR + " are supported, was " + year);
        }
    }

    private static void requireSupported(LocalDate date, String name) {
        Objects.requireNonNull(date, name);
        requireSupportedYear(date.getYear());
    }

    /** True if the date is a Monday to Friday and not a holiday in this calendar. */
    public boolean isWorkingDay(LocalDate date) {
        requireSupported(date, "date");
        boolean weekend = date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
        return !weekend && holidayOn(date).isEmpty();
    }

    /** The name of the holiday on this date, or empty. A holiday on a Saturday or Sunday still has a name. */
    public Optional<String> holidayOn(LocalDate date) {
        requireSupported(date, "date");
        return holidays.stream()
                .filter(holiday -> holiday.dateIn(date.getYear()).filter(date::equals).isPresent())
                .map(Holiday::name)
                .findFirst();
    }

    /**
     * The date reached after {@code days} working days. Working days are counted after {@code start}.
     * With {@code days == 0} the result is {@code start} if it is a working day, otherwise the next working day.
     *
     * @throws IllegalArgumentException if {@code days} is negative
     */
    public LocalDate addWorkingDays(LocalDate start, int days) {
        requireSupported(start, "start");
        if (days < 0) {
            throw new IllegalArgumentException("days must not be negative, was " + days);
        }
        LocalDate date = start;
        if (days == 0) {
            while (!isWorkingDay(date)) {
                date = date.plusDays(1);
            }
            return date;
        }
        // Walk forward one day at a time. The loop ends: a working day comes at least every few days, and
        // LocalDate throws DateTimeException at the end of its range instead of wrapping around.
        int counted = 0;
        while (counted < days) {
            date = date.plusDays(1);
            if (isWorkingDay(date)) {
                counted++;
            }
        }
        return date;
    }

    /**
     * The number of working days in {@code [startInclusive, endExclusive)}.
     *
     * @throws IllegalArgumentException if {@code endExclusive} is before {@code startInclusive}
     */
    public long workingDaysBetween(LocalDate startInclusive, LocalDate endExclusive) {
        Objects.requireNonNull(startInclusive, "startInclusive");
        Objects.requireNonNull(endExclusive, "endExclusive");
        requireSupportedYear(startInclusive.getYear());
        if (endExclusive.isBefore(startInclusive)) {
            throw new IllegalArgumentException("end " + endExclusive + " is before start " + startInclusive);
        }
        long count = 0;
        for (LocalDate date = startInclusive; date.isBefore(endExclusive); date = date.plusDays(1)) {
            if (isWorkingDay(date)) {
                count++;
            }
        }
        return count;
    }

    /** All holiday dates in the year, in date order, including those that fall on a weekend. */
    public List<LocalDate> holidaysIn(int year) {
        requireSupportedYear(year);
        return holidays.stream()
                .map(holiday -> holiday.dateIn(year))
                .flatMap(Optional::stream)
                .distinct()
                .sorted()
                .toList();
    }
}
