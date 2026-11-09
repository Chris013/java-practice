package io.github.chris013.practice.task09;

import java.time.LocalDate;

/** Fixed dates shared by all tests. Never use {@code LocalDate.now()} in a test. */
final class TestDates {

    /**
     * The policy start used by every test unless it says otherwise. The builder defaults are
     * computed from this date, so a test can say "aged 40" without a calendar date.
     */
    static final LocalDate POLICY_START = LocalDate.of(2026, 3, 1);

    private TestDates() {
    }
}
