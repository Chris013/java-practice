package io.github.chris013.practice.task39;

import java.util.Objects;

/**
 * A sensor placed in a room. It implements two interfaces that both declare {@code label()}, so the class must
 * resolve the conflict itself. Otherwise the code does not compile.
 */
public final class RoomSensor implements TemperatureSensor, Labelled, Located {

    private final String id;
    private final String room;
    private final TemperatureSensor source;

    public RoomSensor(String id, String room, TemperatureSensor source) {
        this.id = Objects.requireNonNull(id, "id");
        this.room = Objects.requireNonNull(room, "room");
        this.source = Objects.requireNonNull(source, "source");
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String room() {
        return room;
    }

    @Override
    public double readCelsius() {
        return source.readCelsius();
    }

    /** Picks the room name and the default label from {@link Labelled}, e.g. {@code Kitchen (Sensor t-7)}. */
    @Override
    public String label() {
        return room + " (" + Labelled.super.label() + ")";
    }
}
