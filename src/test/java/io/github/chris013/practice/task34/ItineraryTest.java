package io.github.chris013.practice.task34;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ItineraryTest {

    private static final String DEPARTURE = "2026-05-01T10:00:00Z";
    private static final String RETURN = "2026-05-05T18:00:00Z";

    private static Date at(String isoInstant) {
        return Date.from(Instant.parse(isoInstant));
    }

    private static Itinerary trip() {
        return new Itinerary("Anna", at(DEPARTURE), at(RETURN), new ArrayList<>(List.of("Rome")));
    }

    @Nested
    class Validation {

        @Test
        void accepts_a_day_trip_that_starts_and_ends_at_the_same_instant() {
            assertThatCode(() -> new Itinerary("Anna", at(DEPARTURE), at(DEPARTURE), List.of("Rome")))
                    .doesNotThrowAnyException();
        }

        @Test
        void rejects_a_return_one_millisecond_before_departure() {
            Date oneMillisecondBefore = at("2026-05-01T09:59:59.999Z");

            assertThatThrownBy(() -> new Itinerary("Anna", at(DEPARTURE), oneMillisecondBefore, List.of("Rome")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @CsvSource({"''", "'   '"})
        void rejects_a_blank_traveller(String traveller) {
            assertThatThrownBy(() -> new Itinerary(traveller, at(DEPARTURE), at(RETURN), List.of("Rome")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_no_cities() {
            assertThatThrownBy(() -> new Itinerary("Anna", at(DEPARTURE), at(RETURN), List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @CsvSource({"''", "'   '"})
        void rejects_a_blank_city(String city) {
            assertThatThrownBy(() -> new Itinerary("Anna", at(DEPARTURE), at(RETURN), List.of("Rome", city)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_a_null_city_element() {
            List<String> withNull = Arrays.asList("Rome", null);

            assertThatThrownBy(() -> new Itinerary("Anna", at(DEPARTURE), at(RETURN), withNull))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rejects_null_arguments() {
            assertThatThrownBy(() -> new Itinerary(null, at(DEPARTURE), at(RETURN), List.of("Rome")))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Itinerary("Anna", null, at(RETURN), List.of("Rome")))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Itinerary("Anna", at(DEPARTURE), null, List.of("Rome")))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Itinerary("Anna", at(DEPARTURE), at(RETURN), null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void trims_the_traveller_and_every_city() {
            Itinerary trip = new Itinerary("  Anna  ", at(DEPARTURE), at(RETURN), List.of(" Rome ", " Paris"));

            assertThat(trip.traveller()).isEqualTo("Anna");
            assertThat(trip.cities()).containsExactly("Rome", "Paris");
        }
    }

    @Nested
    class Attacks {

        @Test
        void a_date_passed_in_cannot_change_the_itinerary_afterwards() {
            Date departure = at(DEPARTURE);
            Itinerary trip = new Itinerary("Anna", departure, at(RETURN), List.of("Rome"));

            departure.setTime(Instant.parse("2026-06-01T10:00:00Z").toEpochMilli());

            assertThat(trip.departure()).isEqualTo(at(DEPARTURE));
        }

        @Test
        void a_date_returned_by_a_getter_can_be_changed_without_touching_the_itinerary() {
            Itinerary trip = trip();

            trip.departure().setTime(Instant.parse("2026-06-01T10:00:00Z").toEpochMilli());

            assertThat(trip.departure()).isEqualTo(at(DEPARTURE));
        }

        @Test
        void each_getter_call_returns_a_fresh_date() {
            Itinerary trip = trip();

            assertThat(trip.returnDate()).isNotSameAs(trip.returnDate());
        }

        @Test
        void a_list_passed_in_cannot_change_the_itinerary_afterwards() {
            List<String> cities = new ArrayList<>(List.of("Rome"));
            Itinerary trip = new Itinerary("Anna", at(DEPARTURE), at(RETURN), cities);

            cities.add("Paris");

            assertThat(trip.cities()).containsExactly("Rome");
        }

        @Test
        void the_list_returned_by_cities_is_unmodifiable() {
            assertThatThrownBy(() -> trip().cities().add("Paris"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void a_date_subclass_that_lies_in_clone_cannot_change_the_itinerary() {
            EvilDate departure = new EvilDate(Instant.parse(DEPARTURE).toEpochMilli());
            Itinerary trip = new Itinerary("Anna", departure, at(RETURN), List.of("Rome"));

            departure.setTime(Instant.parse("2026-06-01T10:00:00Z").toEpochMilli());

            assertThat(trip.departure()).isEqualTo(at(DEPARTURE));
            assertThat(trip.durationInDays()).isEqualTo(4);
        }

        @Test
        void validates_and_stores_the_same_value_even_if_a_date_reports_a_different_time_later() {
            // The first reading (10:00 on May 1st) is valid. Every later reading says May 6th, after the return.
            Date departure = new ChangingDate(at(DEPARTURE).getTime(), at("2026-05-06T20:00:00Z").getTime());

            Itinerary trip = new Itinerary("Anna", departure, at(RETURN), List.of("Rome"));

            assertThat(trip.departure()).isEqualTo(at(DEPARTURE));
            assertThat(trip.durationInDays()).isEqualTo(4);
        }

        @Test
        void with_extra_city_returns_a_new_itinerary_and_leaves_the_original_alone() {
            Itinerary original = trip();

            Itinerary extended = original.withExtraCity("Paris");

            assertThat(extended).isNotSameAs(original);
            assertThat(extended.cities()).containsExactly("Rome", "Paris");
            assertThat(original.cities()).containsExactly("Rome");
        }

        @Test
        void with_extra_city_checks_the_new_city() {
            assertThatThrownBy(() -> trip().withExtraCity(" "))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> trip().withExtraCity(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void rescheduled_returns_a_new_itinerary_and_leaves_the_original_alone() {
            Itinerary original = trip();

            Itinerary moved = original.rescheduled(at("2026-06-01T10:00:00Z"), at("2026-06-03T10:00:00Z"));

            assertThat(moved).isNotSameAs(original);
            assertThat(moved.durationInDays()).isEqualTo(2);
            assertThat(original.departure()).isEqualTo(at(DEPARTURE));
            assertThat(original.returnDate()).isEqualTo(at(RETURN));
        }

        @Test
        void rescheduled_validates_the_new_dates() {
            assertThatThrownBy(() -> trip().rescheduled(at(RETURN), at(DEPARTURE)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> trip().rescheduled(null, at(RETURN)))
                    .isInstanceOf(NullPointerException.class);
        }

        /** A Date whose clone() hands back the same object, so a defensive copy made with clone() is no copy at all. */
        private static final class EvilDate extends Date {

            private static final long serialVersionUID = 1L;

            EvilDate(long millis) {
                super(millis);
            }

            @Override
            public Object clone() {
                return this;
            }
        }

        /** Reports one time on its first getTime() call and another time on every call after that. */
        private static final class ChangingDate extends Date {

            private static final long serialVersionUID = 1L;

            private final long firstReading;
            private final long laterReadings;
            private int reads;

            ChangingDate(long firstReading, long laterReadings) {
                super(firstReading);
                this.firstReading = firstReading;
                this.laterReadings = laterReadings;
            }

            @Override
            public long getTime() {
                reads++;
                return reads == 1 ? firstReading : laterReadings;
            }
        }
    }

    @Nested
    class Values {

        @ParameterizedTest
        @CsvSource({
            "2026-05-01T10:00:00Z, 2026-05-01T10:00:00Z, 0",
            "2026-05-01T10:00:00Z, 2026-05-02T09:00:00Z, 0",
            "2026-05-01T10:00:00Z, 2026-05-02T09:59:59.999Z, 0",
            "2026-05-01T10:00:00Z, 2026-05-02T10:00:00Z, 1",
            "2026-05-01T10:00:00Z, 2026-05-04T10:00:00Z, 3"
        })
        void counts_whole_days_between_departure_and_return(String departure, String returnDate, long days) {
            Itinerary trip = new Itinerary("Anna", at(departure), at(returnDate), List.of("Rome"));

            assertThat(trip.durationInDays()).isEqualTo(days);
        }

        @Test
        void equal_when_every_field_is_equal() {
            Itinerary a = trip();
            Itinerary b = trip();

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        void not_equal_when_any_field_differs() {
            Itinerary base = trip();

            assertThat(base).isNotEqualTo(new Itinerary("Ben", at(DEPARTURE), at(RETURN), List.of("Rome")));
            assertThat(base).isNotEqualTo(new Itinerary("Anna", at("2026-05-01T11:00:00Z"), at(RETURN), List.of("Rome")));
            assertThat(base).isNotEqualTo(new Itinerary("Anna", at(DEPARTURE), at(RETURN), List.of("Paris")));
        }

        @Test
        void to_string_shows_the_traveller_and_the_cities() {
            assertThat(trip().toString()).contains("Anna", "Rome");
        }
    }

    @Nested
    class Structure {

        @Test
        void the_class_is_final() {
            assertThat(Modifier.isFinal(Itinerary.class.getModifiers())).isTrue();
        }

        @Test
        void every_field_is_private_and_final() {
            Field[] fields = Itinerary.class.getDeclaredFields();

            assertThat(fields).isNotEmpty();
            assertThat(fields).allSatisfy(field -> {
                assertThat(Modifier.isPrivate(field.getModifiers())).as(field.getName()).isTrue();
                assertThat(Modifier.isFinal(field.getModifiers())).as(field.getName()).isTrue();
            });
        }
    }
}
