package io.github.chris013.practice.task39;

import java.util.Objects;

/**
 * Skeletal implementation (<i>Effective Java</i> Item 20). It takes care of the id and the identity rules, so an
 * implementer only has to write {@link #readCelsius()}.
 *
 * <p>{@code equals}, {@code hashCode} and {@code toString} cannot be default methods in {@link TemperatureSensor}:
 * the compiler rejects a default method that overrides a method of {@link Object}. They have to live in a class.
 */
public abstract class AbstractTemperatureSensor implements TemperatureSensor {

    private final String id;

    protected AbstractTemperatureSensor(String id) {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        this.id = id;
    }

    @Override
    public final String id() {
        return id;
    }

    /** Two sensors are the same when they are of the same class and have the same id. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return id.equals(((AbstractTemperatureSensor) other).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id + "]";
    }
}
