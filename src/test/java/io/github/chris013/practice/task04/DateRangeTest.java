package io.github.chris013.practice.task04;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DateRangeTest {

    private static final LocalDate MAR_1 = LocalDate.of(2026, 3, 1);
    private static final LocalDate MAR_2 = LocalDate.of(2026, 3, 2);
    private static final LocalDate MAR_3 = LocalDate.of(2026, 3, 3);
    private static final LocalDate MAR_4 = LocalDate.of(2026, 3, 4);
    private static final LocalDate MAR_5 = LocalDate.of(2026, 3, 5);
    private static final LocalDate MAR_6 = LocalDate.of(2026, 3, 6);

    @Nested
    class Creation {

        @Test
        void counts_nights_not_days() {
            assertThat(DateRange.of(MAR_1, MAR_4).nights()).isEqualTo(3);
        }

        @Test
        void can_be_created_from_a_number_of_nights() {
            DateRange stay = DateRange.ofNights(MAR_1, 3);

            assertThat(stay.checkOut()).isEqualTo(MAR_4);
            assertThat(stay).isEqualTo(DateRange.of(MAR_1, MAR_4));
        }

        @Test
        void handles_leap_years() {
            assertThat(DateRange.ofNights(LocalDate.of(2028, 2, 28), 2).checkOut()).isEqualTo(LocalDate.of(2028, 3, 1));
        }

        @Test
        void needs_at_least_one_night() {
            assertThatThrownBy(() -> DateRange.of(MAR_1, MAR_1)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> DateRange.of(MAR_4, MAR_1)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> DateRange.ofNights(MAR_1, 0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> DateRange.ofNights(MAR_1, -2)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void refuses_null() {
            assertThatThrownBy(() -> DateRange.of(null, MAR_1)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> DateRange.of(MAR_1, null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void is_equal_by_value() {
            assertThat(DateRange.of(MAR_1, MAR_4))
                    .isEqualTo(DateRange.of(LocalDate.parse("2026-03-01"), LocalDate.parse("2026-03-04")))
                    .hasSameHashCodeAs(DateRange.of(MAR_1, MAR_4))
                    .isNotEqualTo(DateRange.of(MAR_1, MAR_5));
        }
    }

    @Nested
    class Nights {

        private final DateRange stay = DateRange.of(MAR_2, MAR_4);

        @Test
        void include_the_night_of_check_in() {
            assertThat(stay.containsNightOf(MAR_2)).isTrue();
        }

        @Test
        void include_the_last_night_before_check_out() {
            assertThat(stay.containsNightOf(MAR_3)).isTrue();
        }

        @Test
        void exclude_the_night_of_check_out() {
            assertThat(stay.containsNightOf(MAR_4)).isFalse();
        }

        @Test
        void exclude_the_night_before_check_in() {
            assertThat(stay.containsNightOf(MAR_1)).isFalse();
        }

        @Test
        void can_be_listed_as_dates() {
            assertThat(stay.nightsAsDates()).containsExactly(MAR_2, MAR_3);
        }
    }

    @Nested
    class Overlap {

        private final DateRange stay = DateRange.of(MAR_2, MAR_5);

        @Test
        void exists_when_stays_share_a_night() {
            assertThat(stay.overlaps(DateRange.of(MAR_4, MAR_6))).isTrue();
            assertThat(stay.overlaps(DateRange.of(MAR_1, MAR_3))).isTrue();
        }

        @Test
        void exists_when_one_stay_contains_the_other() {
            assertThat(stay.overlaps(DateRange.of(MAR_3, MAR_4))).isTrue();
            assertThat(DateRange.of(MAR_3, MAR_4).overlaps(stay)).isTrue();
            assertThat(stay.overlaps(stay)).isTrue();
        }

        @Test
        void does_not_exist_for_back_to_back_stays() {
            // The first guest checks out on the morning of Mar 5, the next one checks in that afternoon.
            assertThat(stay.overlaps(DateRange.of(MAR_5, MAR_6))).isFalse();
            assertThat(DateRange.of(MAR_5, MAR_6).overlaps(stay)).isFalse();
            assertThat(stay.overlaps(DateRange.of(MAR_1, MAR_2))).isFalse();
        }

        @Test
        void does_not_exist_for_separate_stays() {
            assertThat(DateRange.of(MAR_1, MAR_2).overlaps(DateRange.of(MAR_5, MAR_6))).isFalse();
        }

        @Test
        void has_an_intersection_of_the_shared_nights() {
            assertThat(stay.intersection(DateRange.of(MAR_4, MAR_6))).contains(DateRange.of(MAR_4, MAR_5));
            assertThat(stay.intersection(DateRange.of(MAR_3, MAR_4))).contains(DateRange.of(MAR_3, MAR_4));
        }

        @Test
        void has_no_intersection_for_back_to_back_stays() {
            assertThat(stay.intersection(DateRange.of(MAR_5, MAR_6))).isEmpty();
        }
    }

    @Test
    void sorts_chronologically_by_check_in_then_check_out() {
        DateRange early = DateRange.of(MAR_1, MAR_3);
        DateRange earlyButLonger = DateRange.of(MAR_1, MAR_5);
        DateRange late = DateRange.of(MAR_2, MAR_3);

        List<DateRange> sorted = Stream.of(late, earlyButLonger, early).sorted().toList();

        assertThat(sorted).containsExactly(early, earlyButLonger, late);
    }
}
