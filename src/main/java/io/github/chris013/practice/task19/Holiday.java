package io.github.chris013.practice.task19;

import java.time.LocalDate;
import java.util.Optional;

/**
 * A public holiday. The rule lives here, not in a table of dates: the same object works for 2026 and for 2040.
 */
public interface Holiday {

    /** The name shown to customers, for example {@code "Good Friday"}. */
    String name();

    /**
     * The date of this holiday in the given year, or empty if it does not occur in that year
     * (for example 29 February in a non-leap year).
     */
    Optional<LocalDate> dateIn(int year);
}
