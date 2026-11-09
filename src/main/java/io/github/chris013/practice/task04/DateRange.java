package io.github.chris013.practice.task04;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A stay at our hotel: from the check-in date (inclusive) to the check-out date (exclusive).
 *
 * <p>Task 04: read {@code tasks/04-date-range.md} first. The public API is fixed by the tests; you may turn this into
 * a record.
 */
public final class DateRange implements Comparable<DateRange> {

    /** A stay from {@code checkIn} until {@code checkOut}; the guest leaves on the morning of {@code checkOut}. */
    public static DateRange of(LocalDate checkIn, LocalDate checkOut) {
        throw new UnsupportedOperationException("TODO: Task 04");
    }

    /** A stay of {@code nights} nights starting on {@code checkIn}. */
    public static DateRange ofNights(LocalDate checkIn, int nights) {
        throw new UnsupportedOperationException("TODO: Task 04");
    }

    public LocalDate checkIn() {
        throw new UnsupportedOperationException("TODO: Task 04");
    }

    public LocalDate checkOut() {
        throw new UnsupportedOperationException("TODO: Task 04");
    }

    public long nights() {
        throw new UnsupportedOperationException("TODO: Task 04");
    }

    /** Whether a guest staying in this range sleeps in the hotel during the night starting on {@code date}. */
    public boolean containsNightOf(LocalDate date) {
        throw new UnsupportedOperationException("TODO: Task 04");
    }

    /** Whether two stays need the same room on at least one night. */
    public boolean overlaps(DateRange other) {
        throw new UnsupportedOperationException("TODO: Task 04");
    }

    /** The nights both stays have in common, if any. */
    public Optional<DateRange> intersection(DateRange other) {
        throw new UnsupportedOperationException("TODO: Task 04");
    }

    /** Every night of the stay, identified by the date the night starts on, in order. */
    public Stream<LocalDate> nightsAsDates() {
        throw new UnsupportedOperationException("TODO: Task 04");
    }

    /** Chronological: by check-in, then by check-out. */
    @Override
    public int compareTo(DateRange other) {
        throw new UnsupportedOperationException("TODO: Task 04");
    }
}
