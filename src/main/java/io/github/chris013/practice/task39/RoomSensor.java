package io.github.chris013.practice.task39;

/**
 * A sensor placed in a room. It implements two interfaces that both declare {@code label()}, so the class must
 * resolve the conflict itself. Otherwise the code does not compile.
 *
 * <p>Task 39: read {@code tasks/39-default-methods-sensors.md} first.
 */
public final class RoomSensor implements TemperatureSensor, Labelled, Located {

    public RoomSensor(String id, String room, TemperatureSensor source) {
        throw new UnsupportedOperationException("TODO: Task 39");
    }

    @Override
    public String id() {
        throw new UnsupportedOperationException("TODO: Task 39");
    }

    @Override
    public String room() {
        throw new UnsupportedOperationException("TODO: Task 39");
    }

    @Override
    public double readCelsius() {
        throw new UnsupportedOperationException("TODO: Task 39");
    }

    /** Picks the room name and the default label from {@link Labelled}, e.g. {@code Kitchen (Sensor t-7)}. */
    @Override
    public String label() {
        throw new UnsupportedOperationException("TODO: Task 39");
    }
}
