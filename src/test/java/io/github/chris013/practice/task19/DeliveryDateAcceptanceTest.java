package io.github.chris013.practice.task19;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The outside-in "north star" for this task. It describes what the business needs, in business words.
 * It stays red until the whole feature works. Do not try to make it green in one go: write the small unit tests
 * from your test list first, and come back here when a piece is ready.
 */
class DeliveryDateAcceptanceTest {

    @Test
    void a_three_day_delivery_ordered_before_easter_skips_good_friday_and_easter_monday() {
        // Thu 2 Apr 2026. Fri 3 Apr is Good Friday, Sat and Sun are the weekend, Mon 6 Apr is Easter Monday.
        // Working days: Tue 7, Wed 8, Thu 9.
        WorkingDayCalendar germany = WorkingDayCalendar.germany();

        assertThat(germany.addWorkingDays(LocalDate.of(2026, 4, 2), 3)).isEqualTo(LocalDate.of(2026, 4, 9));
    }

    @Test
    void whit_monday_pushes_a_one_day_delivery_to_tuesday() {
        // Fri 22 May 2026 + 1 working day. Sat and Sun are skipped, and Mon 25 May is Whit Monday.
        WorkingDayCalendar germany = WorkingDayCalendar.germany();

        assertThat(germany.addWorkingDays(LocalDate.of(2026, 5, 22), 1)).isEqualTo(LocalDate.of(2026, 5, 26));
    }

    @Test
    void corpus_christi_is_a_holiday_in_bavaria_but_not_in_berlin() {
        // Corpus Christi 2026 is Thu 4 Jun. A parcel posted on Wed 3 Jun with one working day to go:
        LocalDate posted = LocalDate.of(2026, 6, 3);

        assertThat(WorkingDayCalendar.germany(GermanState.BERLIN).addWorkingDays(posted, 1))
                .isEqualTo(LocalDate.of(2026, 6, 4));
        assertThat(WorkingDayCalendar.germany(GermanState.BAVARIA).addWorkingDays(posted, 1))
                .isEqualTo(LocalDate.of(2026, 6, 5));
    }

    @Test
    void christmas_friday_and_boxing_day_saturday_leave_monday_as_the_next_working_day() {
        // Thu 24 Dec 2026. Fri 25 Dec is Christmas Day, Sat 26 Dec is Boxing Day, Sun 27 Dec is a Sunday.
        WorkingDayCalendar germany = WorkingDayCalendar.germany();

        assertThat(germany.addWorkingDays(LocalDate.of(2026, 12, 24), 1)).isEqualTo(LocalDate.of(2026, 12, 28));
    }

    @Test
    void a_holiday_on_a_weekend_does_not_reduce_the_working_days_of_the_week() {
        // German Unity Day 2026 is Sat 3 Oct. Monday 28 Sep to Monday 5 Oct has exactly five working days.
        WorkingDayCalendar germany = WorkingDayCalendar.germany();

        assertThat(germany.workingDaysBetween(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 5))).isEqualTo(5);
    }

    @Test
    void the_german_holidays_of_2026_are_listed_in_date_order() {
        WorkingDayCalendar germany = WorkingDayCalendar.germany();

        assertThat(germany.holidaysIn(2026)).containsExactly(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 4, 3),
                LocalDate.of(2026, 4, 6),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 14),
                LocalDate.of(2026, 5, 25),
                LocalDate.of(2026, 10, 3),
                LocalDate.of(2026, 12, 25),
                LocalDate.of(2026, 12, 26));
    }

    @Test
    void berlin_has_254_working_days_in_2026() {
        // 261 Monday-to-Friday days in 2026, minus 7 weekday holidays. Berlin's Women's Day (Sun 8 Mar) is on a weekend.
        WorkingDayCalendar berlin = WorkingDayCalendar.germany(GermanState.BERLIN);

        assertThat(berlin.workingDaysBetween(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1))).isEqualTo(254);
    }
}
