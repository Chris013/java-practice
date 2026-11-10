package io.github.chris013.practice.task31;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MeetingInviteTest {

    private static final String ORGANIZER = "anna@x.de";
    private static final String TITLE = "Planning";
    private static final Instant START = Instant.parse("2026-03-02T09:00:00Z");
    private static final Duration THIRTY_MINUTES = Duration.ofMinutes(30);

    private static MeetingInvite.Builder validBuilder() {
        return MeetingInvite.builder(ORGANIZER, TITLE, START, THIRTY_MINUTES);
    }

    private static MeetingInvite validInvite() {
        return validBuilder().build();
    }

    private static MeetingInvite fullInvite() {
        return validBuilder()
                .attendee("bob@x.de")
                .attendee("carla@x.de")
                .location("Room 4")
                .description("Q3 numbers")
                .reminderBefore(Duration.ofMinutes(10))
                .build();
    }

    private static List<String> guests(int count) {
        return IntStream.rangeClosed(1, count).mapToObj(i -> "guest" + i + "@x.de").toList();
    }

    @Nested
    class Defaults {

        @Test
        void an_invite_needs_only_the_required_values() {
            MeetingInvite invite = validInvite();

            assertThat(invite.organizer()).isEqualTo(ORGANIZER);
            assertThat(invite.title()).isEqualTo(TITLE);
            assertThat(invite.start()).isEqualTo(START);
            assertThat(invite.duration()).isEqualTo(THIRTY_MINUTES);
            assertThat(invite.end()).isEqualTo(Instant.parse("2026-03-02T09:30:00Z"));
            assertThat(invite.attendees()).isEmpty();
            assertThat(invite.location()).isEmpty();
            assertThat(invite.description()).isEmpty();
            assertThat(invite.reminderBefore()).isEqualTo(Duration.ofMinutes(15));
        }

        @Test
        void the_location_is_trimmed() {
            assertThat(validBuilder().location("  Room 4 ").build().location()).contains("Room 4");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void a_blank_location_means_no_location(String location) {
            assertThat(validBuilder().location(location).build().location()).isEmpty();
        }

        @Test
        void the_description_is_kept_as_given() {
            assertThat(validBuilder().description("Bring the Q3 numbers").build().description())
                    .isEqualTo("Bring the Q3 numbers");
        }
    }

    @Nested
    class Required {

        @Test
        void the_organizer_must_not_be_null() {
            assertThatNullPointerException()
                    .isThrownBy(() -> MeetingInvite.builder(null, TITLE, START, THIRTY_MINUTES))
                    .withMessage("organizer");
        }

        @Test
        void the_title_must_not_be_null() {
            assertThatNullPointerException()
                    .isThrownBy(() -> MeetingInvite.builder(ORGANIZER, null, START, THIRTY_MINUTES))
                    .withMessage("title");
        }

        @Test
        void the_start_must_not_be_null() {
            assertThatNullPointerException()
                    .isThrownBy(() -> MeetingInvite.builder(ORGANIZER, TITLE, null, THIRTY_MINUTES))
                    .withMessage("start");
        }

        @Test
        void the_duration_must_not_be_null() {
            assertThatNullPointerException()
                    .isThrownBy(() -> MeetingInvite.builder(ORGANIZER, TITLE, START, null))
                    .withMessage("duration");
        }

        @Test
        void a_setter_rejects_null_at_the_line_that_passes_it() {
            MeetingInvite.Builder builder = validBuilder();

            assertThatNullPointerException().isThrownBy(() -> builder.start(null)).withMessage("start");
            assertThatNullPointerException().isThrownBy(() -> builder.duration(null)).withMessage("duration");
            assertThatNullPointerException().isThrownBy(() -> builder.title(null)).withMessage("title");
            assertThatNullPointerException().isThrownBy(() -> builder.attendee(null)).withMessage("email");
            assertThatNullPointerException().isThrownBy(() -> builder.location(null)).withMessage("location");
            assertThatNullPointerException().isThrownBy(() -> builder.description(null)).withMessage("description");
            assertThatNullPointerException()
                    .isThrownBy(() -> builder.reminderBefore(null))
                    .withMessage("reminderBefore");
        }

        @Test
        void the_attendee_collection_and_its_elements_must_not_be_null() {
            MeetingInvite.Builder builder = validBuilder();

            assertThatNullPointerException().isThrownBy(() -> builder.attendees(null)).withMessage("emails");
            assertThatNullPointerException()
                    .isThrownBy(() -> builder.attendees(Arrays.asList("bob@x.de", null)))
                    .withMessage("email");
        }
    }

    @Nested
    class Validation {

        @ParameterizedTest
        @ValueSource(strings = {"PT4M", "PT8H1M", "PT0S", "PT-1M"})
        void rejects_durations_outside_5_minutes_to_8_hours(Duration duration) {
            assertThatThrownBy(() -> validBuilder().duration(duration).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("duration");
        }

        @ParameterizedTest
        @ValueSource(strings = {"PT5M", "PT8H"})
        void accepts_durations_at_the_limits(Duration duration) {
            assertThat(validBuilder().duration(duration).build().duration()).isEqualTo(duration);
        }

        @Test
        void accepts_a_title_of_exactly_100_characters() {
            String title = "x".repeat(100);

            assertThat(validBuilder().title(title).build().title()).isEqualTo(title);
        }

        @Test
        void rejects_a_title_of_101_characters() {
            assertThatThrownBy(() -> validBuilder().title("x".repeat(101)).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("title");
        }

        @Test
        void measures_the_title_after_trimming_it() {
            String title = "x".repeat(100);

            assertThat(validBuilder().title("  " + title + "  ").build().title()).isEqualTo(title);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void rejects_a_blank_title(String title) {
            assertThatThrownBy(() -> validBuilder().title(title).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("title");
        }

        @ParameterizedTest
        @ValueSource(strings = {"PT-1M", "PT24H1M"})
        void rejects_reminders_outside_0_to_24_hours(Duration reminder) {
            assertThatThrownBy(() -> validBuilder().reminderBefore(reminder).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reminder");
        }

        @ParameterizedTest
        @ValueSource(strings = {"PT0S", "PT24H"})
        void accepts_reminders_at_the_limits(Duration reminder) {
            assertThat(validBuilder().reminderBefore(reminder).build().reminderBefore()).isEqualTo(reminder);
        }

        @ParameterizedTest
        @ValueSource(strings = {"anna", "@x.de", "anna@", "a@b@c", "   "})
        void rejects_an_organizer_that_is_not_an_email_address(String email) {
            assertThatThrownBy(() -> MeetingInvite.builder(email, TITLE, START, THIRTY_MINUTES).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("email");
        }

        @ParameterizedTest
        @ValueSource(strings = {"anna", "@x.de", "anna@", "a@b@c", "   "})
        void rejects_an_attendee_that_is_not_an_email_address(String email) {
            assertThatThrownBy(() -> validBuilder().attendee(email).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("email");
        }

        @Test
        void the_bad_email_appears_in_the_message() {
            assertThatThrownBy(() -> validBuilder().attendee("@x.de").build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("@x.de");
        }
    }

    @Nested
    class Attendees {

        @Test
        void trims_and_lower_cases_every_address() {
            assertThat(validBuilder().attendee("  Bob@X.DE ").build().attendees()).containsExactly("bob@x.de");
        }

        @Test
        void drops_duplicates_after_normalization_and_keeps_the_first_position() {
            MeetingInvite invite = validBuilder()
                    .attendee("bob@x.de")
                    .attendee("carla@x.de")
                    .attendee(" BOB@x.de ")
                    .build();

            assertThat(invite.attendees()).containsExactly("bob@x.de", "carla@x.de");
        }

        @Test
        void keeps_the_insertion_order_of_single_and_bulk_additions() {
            MeetingInvite invite = validBuilder()
                    .attendee("carla@x.de")
                    .attendees(List.of("bob@x.de", "dora@x.de"))
                    .attendee("ed@x.de")
                    .build();

            assertThat(invite.attendees()).containsExactly("carla@x.de", "bob@x.de", "dora@x.de", "ed@x.de");
        }

        @Test
        void rejects_the_organizer_as_an_attendee_ignoring_case_and_spaces() {
            assertThatThrownBy(() -> validBuilder().attendee(" ANNA@x.de ").build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("organizer");
        }

        @Test
        void accepts_fifty_attendees() {
            assertThat(validBuilder().attendees(guests(50)).build().attendees()).hasSize(50);
        }

        @Test
        void rejects_fifty_one_attendees() {
            assertThatThrownBy(() -> validBuilder().attendees(guests(51)).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("attendees");
        }

        @Test
        void the_limit_counts_distinct_attendees_only() {
            List<String> withRepeats = new ArrayList<>(guests(50));
            withRepeats.add("GUEST1@x.de");
            withRepeats.add("guest2@x.de");

            assertThat(validBuilder().attendees(withRepeats).build().attendees()).hasSize(50);
        }
    }

    @Nested
    class Immutability {

        @Test
        void the_attendee_list_cannot_be_changed() {
            List<String> attendees = validBuilder().attendee("bob@x.de").build().attendees();

            assertThatThrownBy(() -> attendees.add("eve@x.de")).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void changing_the_builder_after_build_does_not_change_the_invite() {
            MeetingInvite.Builder builder = validBuilder().attendee("bob@x.de");
            MeetingInvite invite = builder.build();

            builder.attendee("carla@x.de").title("Other").location("Room 9");

            assertThat(invite.attendees()).containsExactly("bob@x.de");
            assertThat(invite.title()).isEqualTo(TITLE);
            assertThat(invite.location()).isEmpty();
        }

        @Test
        void building_twice_gives_equal_but_independent_invites() {
            MeetingInvite.Builder builder = validBuilder().attendee("bob@x.de");
            MeetingInvite first = builder.build();

            MeetingInvite second = builder.build();
            builder.attendee("carla@x.de");

            assertThat(second).isEqualTo(first).isNotSameAs(first);
            assertThat(first.attendees()).containsExactly("bob@x.de");
            assertThat(second.attendees()).containsExactly("bob@x.de");
        }
    }

    @Nested
    class ToBuilder {

        @Test
        void a_round_trip_gives_an_equal_invite() {
            MeetingInvite invite = fullInvite();

            MeetingInvite copy = invite.toBuilder().build();

            assertThat(copy).isEqualTo(invite).isNotSameAs(invite);
        }

        @Test
        void rescheduling_changes_only_the_start_and_the_end() {
            MeetingInvite invite = fullInvite();
            Instant tomorrow = START.plus(Duration.ofDays(1));

            MeetingInvite moved = invite.toBuilder().start(tomorrow).build();

            assertThat(moved.start()).isEqualTo(tomorrow);
            assertThat(moved.end()).isEqualTo(tomorrow.plus(THIRTY_MINUTES));
            assertThat(moved.organizer()).isEqualTo(invite.organizer());
            assertThat(moved.title()).isEqualTo(invite.title());
            assertThat(moved.duration()).isEqualTo(invite.duration());
            assertThat(moved.attendees()).isEqualTo(invite.attendees());
            assertThat(moved.location()).isEqualTo(invite.location());
            assertThat(moved.description()).isEqualTo(invite.description());
            assertThat(moved.reminderBefore()).isEqualTo(invite.reminderBefore());
        }

        @Test
        void the_copied_builder_does_not_change_the_original_invite() {
            MeetingInvite invite = fullInvite();

            invite.toBuilder().attendee("dora@x.de").build();

            assertThat(invite.attendees()).containsExactly("bob@x.de", "carla@x.de");
        }
    }

    @Nested
    class Equality {

        @Test
        void invites_with_the_same_values_are_equal_and_have_the_same_hash_code() {
            MeetingInvite first = fullInvite();
            MeetingInvite second = fullInvite();

            assertThat(second).isEqualTo(first);
            assertThat(second).hasSameHashCodeAs(first);
        }

        @Test
        void a_different_description_makes_the_invites_different() {
            MeetingInvite plain = validInvite();
            MeetingInvite described = validBuilder().description("Bring coffee").build();

            assertThat(described).isNotEqualTo(plain);
        }

        @Test
        void to_string_summarises_the_invite() {
            MeetingInvite invite = validBuilder().attendee("bob@x.de").attendee("carla@x.de").build();

            assertThat(invite).hasToString("MeetingInvite[Planning, 2026-03-02T09:00:00Z, 30 min, 2 attendees]");
        }
    }

    @Nested
    class SetterOrder {

        @Test
        void a_value_can_be_corrected_by_a_later_setter() {
            MeetingInvite invite = validBuilder().duration(Duration.ofMinutes(2)).duration(THIRTY_MINUTES).build();

            assertThat(invite.duration()).isEqualTo(THIRTY_MINUTES);
        }

        @Test
        void the_order_of_the_setters_does_not_change_the_invite() {
            MeetingInvite first = validBuilder()
                    .location("Room 4")
                    .reminderBefore(Duration.ofMinutes(10))
                    .attendee("bob@x.de")
                    .description("Q3 numbers")
                    .build();
            MeetingInvite second = validBuilder()
                    .attendee("bob@x.de")
                    .description("Q3 numbers")
                    .reminderBefore(Duration.ofMinutes(10))
                    .location("Room 4")
                    .build();

            assertThat(second).isEqualTo(first);
        }

        @Test
        void a_cross_field_rule_is_checked_by_build_not_by_the_setter() {
            MeetingInvite.Builder builder = validBuilder();

            builder.attendee(ORGANIZER);

            assertThatThrownBy(builder::build)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("organizer");
        }
    }
}
