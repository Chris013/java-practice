package io.github.chris013.practice.task19;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.MonthDay;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The unit tests a TDD session produces, in the order they were written. Nested class names avoid the production
 * type names on purpose, so that {@code Easter} and {@code FixedHoliday} keep meaning the production classes.
 */
class WorkingDayCalendarTest {

    private static final WorkingDayCalendar GERMANY = WorkingDayCalendar.germany();

    @Nested
    class WeekdaysAndWeekends {

        @ParameterizedTest
        @ValueSource(strings = {"2026-03-02", "2026-03-03", "2026-03-04", "2026-03-05", "2026-03-06"})
        void monday_to_friday_are_working_days(String date) {
            assertThat(GERMANY.isWorkingDay(LocalDate.parse(date))).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"2026-03-07", "2026-03-08"})
        void saturday_and_sunday_are_not_working_days(String date) {
            assertThat(GERMANY.isWorkingDay(LocalDate.parse(date))).isFalse();
        }
    }

    @Nested
    class FixedDates {

        @Test
        void a_fixed_holiday_falls_on_the_same_day_every_year() {
            Holiday labourDay = new FixedHoliday("Labour Day", MonthDay.of(5, 1));

            assertThat(labourDay.dateIn(2026)).contains(LocalDate.of(2026, 5, 1));
            assertThat(labourDay.dateIn(2027)).contains(LocalDate.of(2027, 5, 1));
        }

        @Test
        void twenty_ninth_february_does_not_happen_in_a_non_leap_year() {
            Holiday leapDay = new FixedHoliday("Leap Day", MonthDay.of(2, 29));

            assertThat(leapDay.dateIn(2027)).isEmpty();
            assertThat(leapDay.dateIn(2028)).contains(LocalDate.of(2028, 2, 29));
        }

        @Test
        void rejects_a_missing_name_or_day() {
            assertThatThrownBy(() -> new FixedHoliday(null, MonthDay.of(1, 1)))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new FixedHoliday("New Year's Day", null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class EasterSunday {

        // Triangulation: one year can be a lucky constant. Several years from different centuries and
        // different positions in the 19-year cycle force a real computation.
        @ParameterizedTest
        @CsvSource({
            "2000, 2000-04-23",
            "2024, 2024-03-31",
            "2025, 2025-04-20",
            "2026, 2026-04-05",
            "2027, 2027-03-28",
            "2038, 2038-04-25",
            "1818, 1818-03-22",
            "2285, 2285-03-22",
            "1583, 1583-04-10"
        })
        void computes_easter_sunday_for_known_years(int year, String expected) {
            assertThat(Easter.sunday(year)).isEqualTo(LocalDate.parse(expected));
        }

        @Test
        void rejects_years_before_the_gregorian_calendar() {
            assertThatThrownBy(() -> Easter.sunday(1582)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class EasterOffsets {

        @ParameterizedTest
        @CsvSource({
            "-2, 2026-04-03",
            "1, 2026-04-06",
            "39, 2026-05-14",
            "50, 2026-05-25",
            "60, 2026-06-04"
        })
        void counts_days_from_easter_sunday(int offset, String expected) {
            assertThat(new EasterBasedHoliday("Test", offset).dateIn(2026)).contains(LocalDate.parse(expected));
        }
    }

    @Nested
    class NationalHolidays {

        @ParameterizedTest
        @CsvSource({
            "2026-01-01, New Year's Day",
            "2026-04-03, Good Friday",
            "2026-04-06, Easter Monday",
            "2026-05-01, Labour Day",
            "2026-05-14, Ascension Day",
            "2026-05-25, Whit Monday",
            "2026-10-03, German Unity Day",
            "2026-12-25, Christmas Day",
            "2026-12-26, Boxing Day"
        })
        void names_each_national_holiday(String date, String name) {
            assertThat(GERMANY.holidayOn(LocalDate.parse(date))).contains(name);
        }

        @Test
        void a_holiday_on_a_weekend_is_still_named_but_is_not_a_working_day() {
            LocalDate unityDay = LocalDate.of(2026, 10, 3); // a Saturday

            assertThat(GERMANY.holidayOn(unityDay)).contains("German Unity Day");
            assertThat(GERMANY.isWorkingDay(unityDay)).isFalse();
        }

        @Test
        void an_ordinary_weekday_has_no_holiday_name() {
            assertThat(GERMANY.holidayOn(LocalDate.of(2026, 3, 4))).isEmpty();
        }
    }

    @Nested
    class StateHolidays {

        @ParameterizedTest
        @EnumSource(GermanState.class)
        void every_state_has_the_national_holidays(GermanState state) {
            assertThat(WorkingDayCalendar.germany(state).holidayOn(LocalDate.of(2026, 5, 14)))
                    .contains("Ascension Day");
        }

        @ParameterizedTest
        @CsvSource({
            "BAVARIA, 2026-01-06, Epiphany",
            "BAVARIA, 2026-08-15, Assumption Day",
            "BAVARIA, 2026-11-01, All Saints' Day",
            "BERLIN, 2026-03-08, International Women's Day",
            "NORTH_RHINE_WESTPHALIA, 2026-11-01, All Saints' Day"
        })
        void names_the_holidays_of_each_state(GermanState state, String date, String name) {
            assertThat(WorkingDayCalendar.germany(state).holidayOn(LocalDate.parse(date))).contains(name);
        }

        @ParameterizedTest
        @CsvSource({"BAVARIA, true", "NORTH_RHINE_WESTPHALIA, true", "BERLIN, false"})
        void corpus_christi_is_a_holiday_only_in_the_states_that_observe_it(GermanState state, boolean holiday) {
            LocalDate corpusChristi2026 = LocalDate.of(2026, 6, 4);

            assertThat(WorkingDayCalendar.germany(state).holidayOn(corpusChristi2026).isPresent()).isEqualTo(holiday);
        }

        @Test
        void a_state_holiday_is_not_a_holiday_in_the_national_calendar() {
            assertThat(GERMANY.isWorkingDay(LocalDate.of(2026, 1, 6))).isTrue();
            assertThat(WorkingDayCalendar.germany(GermanState.BAVARIA).isWorkingDay(LocalDate.of(2026, 1, 6))).isFalse();
        }
    }

    @Nested
    class AddingWorkingDays {

        @ParameterizedTest
        @CsvSource({
            "2026-03-02, 1, 2026-03-03",
            "2026-03-06, 1, 2026-03-09",
            "2026-03-06, 5, 2026-03-13",
            "2026-03-07, 1, 2026-03-09",
            "2026-04-02, 1, 2026-04-07",
            "2026-04-02, 3, 2026-04-09",
            "2025-12-31, 1, 2026-01-02"
        })
        void moves_forward_by_working_days_only(String start, int days, String expected) {
            assertThat(GERMANY.addWorkingDays(LocalDate.parse(start), days)).isEqualTo(LocalDate.parse(expected));
        }

        @Test
        void zero_days_returns_a_working_day_unchanged() {
            assertThat(GERMANY.addWorkingDays(LocalDate.of(2026, 3, 4), 0)).isEqualTo(LocalDate.of(2026, 3, 4));
        }

        @ParameterizedTest
        @CsvSource({
            "2026-03-07, 2026-03-09",
            "2026-12-25, 2026-12-28"
        })
        void zero_days_on_a_non_working_day_returns_the_next_working_day(String start, String expected) {
            assertThat(GERMANY.addWorkingDays(LocalDate.parse(start), 0)).isEqualTo(LocalDate.parse(expected));
        }

        @Test
        void rejects_negative_days() {
            assertThatThrownBy(() -> GERMANY.addWorkingDays(LocalDate.of(2026, 3, 4), -1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class CountingWorkingDays {

        @Test
        void excludes_the_end_date() {
            assertThat(GERMANY.workingDaysBetween(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 3))).isEqualTo(1);
        }

        @Test
        void counts_a_full_week_as_five_working_days() {
            assertThat(GERMANY.workingDaysBetween(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 9))).isEqualTo(5);
        }

        @Test
        void returns_zero_for_an_empty_range() {
            assertThat(GERMANY.workingDaysBetween(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 2))).isZero();
        }

        @Test
        void skips_good_friday_and_easter_monday() {
            // Mon 30 Mar to Tue 7 Apr: Mon, Tue, Wed, Thu count; Good Friday, weekend and Easter Monday do not.
            assertThat(GERMANY.workingDaysBetween(LocalDate.of(2026, 3, 30), LocalDate.of(2026, 4, 7))).isEqualTo(4);
        }

        @Test
        void rejects_an_end_before_the_start() {
            assertThatThrownBy(() -> GERMANY.workingDaysBetween(LocalDate.of(2026, 3, 9), LocalDate.of(2026, 3, 2)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @CsvSource({"BAVARIA, 252", "BERLIN, 254", "NORTH_RHINE_WESTPHALIA, 253"})
        void counts_a_whole_year_per_state(GermanState state, long expected) {
            assertThat(WorkingDayCalendar.germany(state)
                    .workingDaysBetween(LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1))).isEqualTo(expected);
        }
    }

    @Nested
    class HolidaysInYear {

        @ParameterizedTest
        @CsvSource({"BAVARIA, 13", "BERLIN, 10", "NORTH_RHINE_WESTPHALIA, 11"})
        void lists_every_holiday_of_the_year_once(GermanState state, int expected) {
            assertThat(WorkingDayCalendar.germany(state).holidaysIn(2026)).hasSize(expected).doesNotHaveDuplicates();
        }

        @Test
        void lists_holidays_in_date_order_including_those_on_weekends() {
            assertThat(GERMANY.holidaysIn(2026)).hasSize(9).isSorted().contains(LocalDate.of(2026, 10, 3));
        }
    }

    @Nested
    class ArgumentChecks {

        @Test
        void rejects_null_arguments() {
            assertThatThrownBy(() -> GERMANY.isWorkingDay(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> GERMANY.holidayOn(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> GERMANY.addWorkingDays(null, 1)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> WorkingDayCalendar.germany(null)).isInstanceOf(NullPointerException.class);
        }
    }
}
