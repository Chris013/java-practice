package io.github.chris013.practice.task19;

import java.time.LocalDate;
import java.util.Optional;

/**
 * A holiday defined as a number of days before or after Easter Sunday, such as Good Friday ({@code -2}).
 *
 * <p>Task 19: read {@code tasks/19-tdd-working-days.md} first.
 */
public record EasterBasedHoliday(String name, int daysAfterEasterSunday) implements Holiday {

    @Override
    public Optional<LocalDate> dateIn(int year) {
        throw new UnsupportedOperationException("TODO: Task 19");
    }
}
