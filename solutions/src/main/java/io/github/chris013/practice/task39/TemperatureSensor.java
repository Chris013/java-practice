package io.github.chris013.practice.task39;

import java.util.Objects;

/**
 * A temperature sensor as published by the platform team. Implementers in other teams' code only provide
 * {@link #id()} and {@link #readCelsius()}. Everything else is a default method, so we could add it without
 * breaking them.
 */
public interface TemperatureSensor {

    String id();

    double readCelsius();

    /** Fahrenheit is derived from Celsius unless the implementation has a native Fahrenheit reading. */
    default double readFahrenheit() {
        return readCelsius() * 9 / 5 + 32;
    }

    /** The boundary is inclusive: water at exactly 0.0 C counts as freezing. */
    default boolean isFreezing() {
        return readCelsius() <= 0.0;
    }

    /**
     * Returns a sensor with the same id whose reading is this sensor's reading plus {@code offsetCelsius}.
     *
     * <p>The result reads through to this sensor on every call. A snapshot would silently stop following
     * the hardware after the first call.
     */
    default TemperatureSensor calibrated(double offsetCelsius) {
        if (Double.isNaN(offsetCelsius)) {
            throw new IllegalArgumentException("offsetCelsius must be a number");
        }
        TemperatureSensor source = this;
        return new TemperatureSensor() {
            @Override
            public String id() {
                return source.id();
            }

            @Override
            public double readCelsius() {
                return source.readCelsius() + offsetCelsius;
            }
        };
    }

    /**
     * A sensor that always reports the same value. Useful for tests and for stubbing out hardware.
     *
     * @throws IllegalArgumentException if {@code id} is blank
     */
    static TemperatureSensor fixed(String id, double celsius) {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        return new SimulatedSensor(id, () -> celsius);
    }
}
