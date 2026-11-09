package io.github.chris013.practice.task17;

import java.util.function.Consumer;

/**
 * An in-process publish/subscribe bus for domain events. Modules subscribe to the event types they care about
 * and never import each other's classes.
 *
 * <p>Task 17: read {@code tasks/17-type-safe-event-bus.md} first.
 */
public final class EventBus {

    /**
     * Registers {@code handler} for {@code eventType} and for every subtype of it. Subscribing to {@code Object.class}
     * receives every event.
     *
     * <p>Handlers run in the order in which they were subscribed, across all event types.
     *
     * @return a subscription that stops the delivery when closed
     */
    public <E> Subscription subscribe(Class<E> eventType, Consumer<? super E> handler) {
        throw new UnsupportedOperationException("TODO: Task 17");
    }

    /**
     * Delivers {@code event} to every matching handler, in subscription order.
     *
     * <p>A failing handler does not stop the others. After all of them ran, if any threw a
     * {@link RuntimeException}, this method throws an {@link EventDeliveryException}. If no handler matched at all,
     * the bus publishes a {@link DeadEvent} instead.
     *
     * @throws NullPointerException if {@code event} is null
     * @throws EventDeliveryException if at least one handler failed
     */
    public void publish(Object event) {
        throw new UnsupportedOperationException("TODO: Task 17");
    }

    /**
     * Publishes each event in iteration order, one {@link #publish} after the other. Stops at the first event whose
     * delivery fails.
     *
     * @throws EventDeliveryException if a handler failed for one of the events
     */
    public void publishAll(Iterable<?> events) {
        throw new UnsupportedOperationException("TODO: Task 17");
    }
}
