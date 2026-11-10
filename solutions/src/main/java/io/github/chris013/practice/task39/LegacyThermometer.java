package io.github.chris013.practice.task39;

import java.util.Objects;

/**
 * Given. Written by another team years ago, compiled against the first version of {@link TemperatureSensor}
 * which only had {@code id()} and {@code readCelsius()}. We cannot recompile it in lockstep with us.
 */
public final class LegacyThermometer implements TemperatureSensor {

    private final String id;
    private double celsius;

    public LegacyThermometer(String id, double celsius) {
        this.id = Objects.requireNonNull(id, "id");
        this.celsius = celsius;
    }

    /** Simulates the hardware reporting a new value. */
    public void update(double celsius) {
        this.celsius = celsius;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public double readCelsius() {
        return celsius;
    }
}
