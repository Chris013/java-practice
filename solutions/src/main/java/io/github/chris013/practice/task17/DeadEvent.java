package io.github.chris013.practice.task17;

import java.util.Objects;

/**
 * Given. Published by the bus when an event had no subscriber at all, so that someone can log it or alert on it.
 * A {@code DeadEvent} that nobody listens to is dropped silently.
 */
public record DeadEvent(Object event) {

    public DeadEvent {
        Objects.requireNonNull(event, "event");
    }
}
