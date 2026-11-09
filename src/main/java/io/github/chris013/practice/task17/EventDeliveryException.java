package io.github.chris013.practice.task17;

/**
 * Given. Thrown by {@link EventBus#publish} after every matching handler has run, when at least one of them failed.
 * The first failure is the {@linkplain #getCause() cause}; the later ones are {@linkplain #getSuppressed() suppressed}.
 */
public final class EventDeliveryException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    // Transient: the event may not be serializable, and the exception is only used in-process.
    private final transient Object event;

    EventDeliveryException(Object event, RuntimeException firstFailure) {
        super("A handler failed while delivering " + event.getClass().getName(), firstFailure);
        this.event = event;
    }

    /** The event that could not be delivered to every handler. */
    public Object event() {
        return event;
    }
}
