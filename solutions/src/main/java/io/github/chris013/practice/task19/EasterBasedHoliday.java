package io.github.chris013.practice.task19;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * A holiday defined as a number of days before or after Easter Sunday, such as Good Friday ({@code -2}).
 * Such a holiday happens every year, so {@link #dateIn} is never empty.
 */
public record EasterBasedHoliday(String name, int daysAfterEasterSunday) implements Holiday {

    public EasterBasedHoliday {
        Objects.requireNonNull(name, "name");
    }

    @Override
    public Optional<LocalDate> dateIn(int year) {
        return Optional.of(Easter.sunday(year).plusDays(daysAfterEasterSunday));
    }
}
