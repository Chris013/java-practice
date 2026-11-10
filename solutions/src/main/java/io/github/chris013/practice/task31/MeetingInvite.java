package io.github.chris013.practice.task31;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A meeting invitation. Once it exists, it is valid and it never changes.
 *
 * <p>Create one with {@link #builder(String, String, Instant, Duration)}. To reschedule an existing invite,
 * use {@link #toBuilder()}. The rules that involve several fields are checked in {@link Builder#build()},
 * so the order of the setter calls never matters.
 */
public final class MeetingInvite {

    private static final Duration MIN_DURATION = Duration.ofMinutes(5);
    private static final Duration MAX_DURATION = Duration.ofHours(8);
    private static final Duration DEFAULT_REMINDER = Duration.ofMinutes(15);
    private static final Duration MAX_REMINDER = Duration.ofHours(24);
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_ATTENDEES = 50;

    private final String organizer;
    private final String title;
    private final Instant start;
    private final Duration duration;
    private final List<String> attendees;
    private final String location; // null means "no location"; exposed as Optional
    private final String description;
    private final Duration reminderBefore;

    /**
     * Only reachable through {@link Builder#build()}. All checks run here, on the builder's
     * values. The attendee list is copied, so later changes to the builder cannot reach this invite.
     */
    private MeetingInvite(Builder builder) {
        this.organizer = normalizeEmail(builder.organizer);
        this.title = validTitle(builder.title);
        this.start = builder.start;
        this.duration = validDuration(builder.duration);
        this.attendees = normalizeAttendees(organizer, builder.attendees);
        this.location = normalizeLocation(builder.location);
        this.description = builder.description;
        this.reminderBefore = validReminder(builder.reminderBefore);
    }

    /**
     * Required values are parameters, so a caller cannot forget them. Optional values are setters
     * because a long parameter list is where swapped arguments come from.
     */
    public static Builder builder(String organizer, String title, Instant start, Duration duration) {
        return new Builder(organizer, title, start, duration);
    }

    public String organizer() {
        return organizer;
    }

    public String title() {
        return title;
    }

    public Instant start() {
        return start;
    }

    public Duration duration() {
        return duration;
    }

    /** {@code start() + duration()}. Derived, so it is never stored and can never disagree with the others. */
    public Instant end() {
        return start.plus(duration);
    }

    public List<String> attendees() {
        return attendees;
    }

    public Optional<String> location() {
        return Optional.ofNullable(location);
    }

    public String description() {
        return description;
    }

    public Duration reminderBefore() {
        return reminderBefore;
    }

    public Builder toBuilder() {
        Builder copy = new Builder(organizer, title, start, duration);
        copy.attendees.addAll(attendees);
        copy.location = location;
        copy.description = description;
        copy.reminderBefore = reminderBefore;
        return copy;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MeetingInvite that)) {
            return false;
        }
        return organizer.equals(that.organizer)
                && title.equals(that.title)
                && start.equals(that.start)
                && duration.equals(that.duration)
                && attendees.equals(that.attendees)
                && Objects.equals(location, that.location)
                && description.equals(that.description)
                && reminderBefore.equals(that.reminderBefore);
    }

    @Override
    public int hashCode() {
        return Objects.hash(organizer, title, start, duration, attendees, location, description, reminderBefore);
    }

    @Override
    public String toString() {
        return "MeetingInvite[%s, %s, %d min, %d attendees]"
                .formatted(title, start, duration.toMinutes(), attendees.size());
    }

    private static String validTitle(String raw) {
        String title = raw.strip();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                    "title must be at most %d characters, was %d".formatted(MAX_TITLE_LENGTH, title.length()));
        }
        return title;
    }

    private static Duration validDuration(Duration duration) {
        if (duration.compareTo(MIN_DURATION) < 0 || duration.compareTo(MAX_DURATION) > 0) {
            throw new IllegalArgumentException(
                    "duration must be between %s and %s, was %s".formatted(MIN_DURATION, MAX_DURATION, duration));
        }
        return duration;
    }

    private static Duration validReminder(Duration reminder) {
        if (reminder.isNegative() || reminder.compareTo(MAX_REMINDER) > 0) {
            throw new IllegalArgumentException(
                    "reminder must be between 0 and %s, was %s".formatted(MAX_REMINDER, reminder));
        }
        return reminder;
    }

    /**
     * Trims and lower-cases the address, then checks that it has exactly one {@code @} with text on
     * both sides. This is not full RFC 5322 validation, it only catches the typos that happen in practice.
     * We normalize before comparing, so {@code Bob@X.de} and {@code bob@x.de} are the same person.
     */
    private static String normalizeEmail(String raw) {
        String email = raw.strip().toLowerCase(Locale.ROOT);
        int at = email.indexOf('@');
        boolean valid = at > 0 && at < email.length() - 1 && email.indexOf('@', at + 1) < 0;
        if (!valid) {
            throw new IllegalArgumentException("email is not an address: '" + raw + "'");
        }
        return email;
    }

    /**
     * Normalizes and de-duplicates before counting, so the 50-attendee limit applies to people and
     * not to typed-in duplicates. A {@link LinkedHashSet} keeps the first position of each address.
     */
    private static List<String> normalizeAttendees(String organizer, List<String> raw) {
        Set<String> distinct = new LinkedHashSet<>();
        for (String email : raw) {
            distinct.add(normalizeEmail(email));
        }
        if (distinct.contains(organizer)) {
            throw new IllegalArgumentException("organizer " + organizer + " cannot also be an attendee");
        }
        if (distinct.size() > MAX_ATTENDEES) {
            throw new IllegalArgumentException(
                    "attendees must be at most %d after removing duplicates, was %d"
                            .formatted(MAX_ATTENDEES, distinct.size()));
        }
        // List.copyOf makes an unmodifiable copy, so no one can reach the set we just built.
        return List.copyOf(distinct);
    }

    /** A blank location means "no location". Nothing is ever stored as an empty string. */
    private static String normalizeLocation(String raw) {
        if (raw == null) {
            return null;
        }
        String location = raw.strip();
        return location.isEmpty() ? null : location;
    }

    /**
     * The mutable state of an invite under construction. Every setter validates only its own
     * argument (null). The rules that need several fields are checked in {@link #build()}.
     */
    public static final class Builder {

        private final String organizer;
        private String title;
        private Instant start;
        private Duration duration;
        private final List<String> attendees = new ArrayList<>(); // raw, not yet normalized
        private String location;
        private String description = "";
        private Duration reminderBefore = DEFAULT_REMINDER;

        /**
         * Null checks run now, on the line that passes the wrong value, not later in {@link #build()}.
         */
        private Builder(String organizer, String title, Instant start, Duration duration) {
            this.organizer = Objects.requireNonNull(organizer, "organizer");
            this.title = Objects.requireNonNull(title, "title");
            this.start = Objects.requireNonNull(start, "start");
            this.duration = Objects.requireNonNull(duration, "duration");
        }

        public Builder start(Instant start) {
            this.start = Objects.requireNonNull(start, "start");
            return this;
        }

        public Builder duration(Duration duration) {
            this.duration = Objects.requireNonNull(duration, "duration");
            return this;
        }

        public Builder title(String title) {
            this.title = Objects.requireNonNull(title, "title");
            return this;
        }

        public Builder attendee(String email) {
            attendees.add(Objects.requireNonNull(email, "email"));
            return this;
        }

        public Builder attendees(Collection<String> emails) {
            List<String> checked = new ArrayList<>(Objects.requireNonNull(emails, "emails"));
            checked.forEach(email -> Objects.requireNonNull(email, "email"));
            attendees.addAll(checked); // all or nothing: a null element leaves the builder unchanged
            return this;
        }

        public Builder location(String location) {
            this.location = Objects.requireNonNull(location, "location");
            return this;
        }

        public Builder description(String description) {
            this.description = Objects.requireNonNull(description, "description");
            return this;
        }

        public Builder reminderBefore(Duration reminderBefore) {
            this.reminderBefore = Objects.requireNonNull(reminderBefore, "reminderBefore");
            return this;
        }

        /**
         * Returns a new, independent invite each time. The builder is not modified, so it can
         * be used again.
         */
        public MeetingInvite build() {
            return new MeetingInvite(this);
        }
    }
}
