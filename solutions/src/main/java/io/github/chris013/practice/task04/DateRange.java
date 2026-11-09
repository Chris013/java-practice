package io.github.chris013.practice.task04;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A stay at our hotel: from the check-in date (inclusive) to the check-out date (exclusive), at least one night long.
 *
 * <p>The range is half-open, {@code [checkIn, checkOut)}, because that is how hotels think: the night of the
 * check-out date belongs to the next guest. As a consequence back-to-back stays never overlap, and the number of
 * nights is simply the distance between the two dates.
 */
public record DateRange(LocalDate checkIn, LocalDate checkOut) implements Comparable<DateRange> {

    private static final Comparator<DateRange> CHRONOLOGICAL =
            Comparator.comparing(DateRange::checkIn).thenComparing(DateRange::checkOut);

    public DateRange {
        Objects.requireNonNull(checkIn, "checkIn");
        Objects.requireNonNull(checkOut, "checkOut");
        if (!checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException(
                    "A stay needs at least one night, but check-out %s is not after check-in %s".formatted(checkOut, checkIn));
        }
    }

    public static DateRange of(LocalDate checkIn, LocalDate checkOut) {
        return new DateRange(checkIn, checkOut);
    }

    public static DateRange ofNights(LocalDate checkIn, int nights) {
        Objects.requireNonNull(checkIn, "checkIn");
        return new DateRange(checkIn, checkIn.plusDays(nights));
    }

    public long nights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public boolean containsNightOf(LocalDate date) {
        return !date.isBefore(checkIn) && date.isBefore(checkOut);
    }

    public boolean overlaps(DateRange other) {
        return checkIn.isBefore(other.checkOut) && other.checkIn.isBefore(checkOut);
    }

    public Optional<DateRange> intersection(DateRange other) {
        if (!overlaps(other)) {
            return Optional.empty();
        }
        return Optional.of(new DateRange(later(checkIn, other.checkIn), earlier(checkOut, other.checkOut)));
    }

    public Stream<LocalDate> nightsAsDates() {
        return checkIn.datesUntil(checkOut);
    }

    @Override
    public int compareTo(DateRange other) {
        return CHRONOLOGICAL.compare(this, other);
    }

    private static LocalDate later(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate earlier(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
