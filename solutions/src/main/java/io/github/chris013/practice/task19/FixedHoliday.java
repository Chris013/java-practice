package io.github.chris013.practice.task19;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Objects;
import java.util.Optional;

/**
 * A holiday on the same calendar day every year, such as 3 October.
 */
public record FixedHoliday(String name, MonthDay day) implements Holiday {

    public FixedHoliday {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(day, "day");
    }

    /** Empty for 29 February in a non-leap year: the holiday simply does not happen that year. */
    @Override
    public Optional<LocalDate> dateIn(int year) {
        return day.isValidYear(year) ? Optional.of(day.atYear(year)) : Optional.empty();
    }
}
