package io.github.chris013.practice.task17;

/**
 * Given. The handle returned by {@link EventBus#subscribe}. Closing it stops the delivery of events to the handler.
 * Closing is idempotent, and it does not throw a checked exception, so it works in a try-with-resources block.
 */
@FunctionalInterface
public interface Subscription extends AutoCloseable {

    @Override
    void close();
}
