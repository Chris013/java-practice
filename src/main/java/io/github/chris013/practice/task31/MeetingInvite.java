package io.github.chris013.practice.task31;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * A meeting invitation. Once it exists, it is valid and it never changes.
 *
 * <p>Create one with {@link #builder(String, String, Instant, Duration)}. To reschedule an existing invite,
 * use {@link #toBuilder()}. The builder checks the rules that involve several fields in {@link Builder#build()}.
 *
 * <p>Task 31: read {@code tasks/31-meeting-invite-builder.md} first.
 */
public final class MeetingInvite {

    private MeetingInvite(Builder builder) {
        throw new UnsupportedOperationException("TODO: Task 31");
    }

    /** Required values go into the factory, optional values go into the builder's setters. */
    public static Builder builder(String organizer, String title, Instant start, Duration duration) {
        throw new UnsupportedOperationException("TODO: Task 31");
    }

    public String organizer() {
        throw new UnsupportedOperationException("TODO: Task 31");
    }

    public String title() {
        throw new UnsupportedOperationException("TODO: Task 31");
    }

    public Instant start() {
        throw new UnsupportedOperationException("TODO: Task 31");
    }

    public Duration duration() {
        throw new UnsupportedOperationException("TODO: Task 31");
    }

    /** {@code start() + duration()}. */
    public Instant end() {
        throw new UnsupportedOperationException("TODO: Task 31");
    }

    /** Unmodifiable, in insertion order, without duplicates. */
    public List<String> attendees() {
        throw new UnsupportedOperationException("TODO: Task 31");
    }

    public Optional<String> location() {
        throw new UnsupportedOperationException("TODO: Task 31");
    }

    /** Empty when no description was given. */
    public String description() {
        throw new UnsupportedOperationException("TODO: Task 31");
    }

    /** How long before the start the reminder is sent. Zero means no reminder. */
    public Duration reminderBefore() {
        throw new UnsupportedOperationException("TODO: Task 31");
    }

    /** A builder pre-filled with every value of this invite. Changing it never changes this invite. */
    public Builder toBuilder() {
        throw new UnsupportedOperationException("TODO: Task 31");
    }

    @Override
    public boolean equals(Object other) {
        throw new UnsupportedOperationException("TODO: Task 31");
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException("TODO: Task 31");
    }

    /** For example {@code MeetingInvite[Planning, 2026-03-02T09:00:00Z, 30 min, 2 attendees]}. */
    @Override
    public String toString() {
        throw new UnsupportedOperationException("TODO: Task 31");
    }

    /**
     * Collects the values of one invite. Setters return {@code this}, so calls can be chained.
     * Null arguments fail at the setter. The other rules are checked by {@link #build()}.
     */
    public static final class Builder {

        private Builder(String organizer, String title, Instant start, Duration duration) {
            throw new UnsupportedOperationException("TODO: Task 31");
        }

        public Builder start(Instant start) {
            throw new UnsupportedOperationException("TODO: Task 31");
        }

        public Builder duration(Duration duration) {
            throw new UnsupportedOperationException("TODO: Task 31");
        }

        public Builder title(String title) {
            throw new UnsupportedOperationException("TODO: Task 31");
        }

        /** Adds one attendee. You can call this many times. */
        public Builder attendee(String email) {
            throw new UnsupportedOperationException("TODO: Task 31");
        }

        /** Adds all the given attendees, in order. */
        public Builder attendees(Collection<String> emails) {
            throw new UnsupportedOperationException("TODO: Task 31");
        }

        public Builder location(String location) {
            throw new UnsupportedOperationException("TODO: Task 31");
        }

        public Builder description(String description) {
            throw new UnsupportedOperationException("TODO: Task 31");
        }

        public Builder reminderBefore(Duration reminderBefore) {
            throw new UnsupportedOperationException("TODO: Task 31");
        }

        /** Checks the rules and returns a new invite. The builder can be used again. */
        public MeetingInvite build() {
            throw new UnsupportedOperationException("TODO: Task 31");
        }
    }
}
