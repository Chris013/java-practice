package io.github.chris013.practice.task39;

/**
 * Skeletal implementation (<i>Effective Java</i> Item 20). It takes care of the id and the identity rules, so an
 * implementer only has to write {@link #readCelsius()}.
 *
 * <p>{@code equals}, {@code hashCode} and {@code toString} cannot be default methods in {@link TemperatureSensor}:
 * the compiler rejects a default method that overrides a method of {@link Object}. They have to live in a class.
 *
 * <p>Task 39: read {@code tasks/39-default-methods-sensors.md} first.
 */
public abstract class AbstractTemperatureSensor implements TemperatureSensor {

    protected AbstractTemperatureSensor(String id) {
        throw new UnsupportedOperationException("TODO: Task 39");
    }

    @Override
    public final String id() {
        throw new UnsupportedOperationException("TODO: Task 39");
    }

    /** Two sensors are the same when they are of the same class and have the same id. */
    @Override
    public boolean equals(Object other) {
        throw new UnsupportedOperationException("TODO: Task 39");
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException("TODO: Task 39");
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException("TODO: Task 39");
    }
}
