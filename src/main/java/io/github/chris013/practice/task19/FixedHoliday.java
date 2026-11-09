package io.github.chris013.practice.task19;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Optional;

/**
 * A holiday on the same calendar day every year, such as 3 October.
 *
 * <p>Task 19: read {@code tasks/19-tdd-working-days.md} first.
 */
public record FixedHoliday(String name, MonthDay day) implements Holiday {

    @Override
    public Optional<LocalDate> dateIn(int year) {
        throw new UnsupportedOperationException("TODO: Task 19");
    }
}
