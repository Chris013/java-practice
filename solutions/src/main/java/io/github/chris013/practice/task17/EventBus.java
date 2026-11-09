package io.github.chris013.practice.task17;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * An in-process publish/subscribe bus for domain events. Modules subscribe to the event types they care about
 * and never import each other's classes.
 *
 * <p>Thread safety: {@code subscribe}, {@code close} and {@code publish} may run on different threads. The
 * registrations live in a {@link CopyOnWriteArrayList}, so reads (publishing) are lock-free and every publish
 * delivers to a snapshot. That is why a handler that subscribes or closes during a publish does not change the
 * delivery in progress, and why nothing ever throws {@code ConcurrentModificationException}.
 * Writes copy the whole list, which is fine because subscriptions change rarely compared to publishing.
 */
public final class EventBus {

    // One list for all types: its insertion order is the subscription order, which gives us the global ordering.
    private final List<Registration<?>> registrations = new CopyOnWriteArrayList<>();

    public <E> Subscription subscribe(Class<E> eventType, Consumer<? super E> handler) {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(handler, "handler");
        Registration<E> registration = new Registration<>(eventType, handler);
        registrations.add(registration);
        // Remove by identity. Two registrations of the same type and handler are equal records, and closing one of
        // them must not remove the other.
        return () -> registrations.removeIf(existing -> existing == registration);
    }

    public void publish(Object event) {
        Objects.requireNonNull(event, "event");
        boolean delivered = deliverToMatchingHandlers(event);
        if (!delivered && !(event instanceof DeadEvent)) {
            // Only a DeadEvent that nobody listens to is dropped. Wrapping it again would recurse forever.
            publish(new DeadEvent(event));
        }
    }

    public void publishAll(Iterable<?> events) {
        Objects.requireNonNull(events, "events");
        for (Object event : events) {
            publish(event);
        }
    }

    /**
     * Runs every matching handler, even after a failure. Returns whether at least one handler matched.
     * Only {@link RuntimeException}s are collected: an {@link Error} means the JVM is in trouble, and we let it
     * propagate at once.
     */
    private boolean deliverToMatchingHandlers(Object event) {
        boolean matched = false;
        EventDeliveryException failure = null;
        for (Registration<?> registration : registrations) {
            try {
                matched |= registration.deliverIfMatching(event);
            } catch (RuntimeException handlerFailure) {
                if (failure == null) {
                    failure = new EventDeliveryException(event, handlerFailure);
                } else {
                    failure.addSuppressed(handlerFailure);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
        return matched;
    }

    /**
     * One subscription: an event type and the handler for it.
     *
     * <p>How the types line up without unchecked casts: the bus stores {@code Registration<?>} because it cannot name
     * the event type of each subscription. Inside {@code deliverIfMatching} the compiler sees one {@code E} that
     * both {@code type} and {@code handler} share, so {@code type.cast(event)} returns an {@code E} that the
     * {@code Consumer<? super E>} accepts. The cast is checked at run time by {@code Class.cast}, and we call
     * {@code isInstance} first so that a non-matching event is skipped instead of throwing.
     */
    private record Registration<E>(Class<E> type, Consumer<? super E> handler) {

        boolean deliverIfMatching(Object event) {
            if (!type.isInstance(event)) {
                return false;
            }
            handler.accept(type.cast(event));
            return true;
        }
    }
}
