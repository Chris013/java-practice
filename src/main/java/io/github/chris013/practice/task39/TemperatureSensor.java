package io.github.chris013.practice.task39;

/**
 * A temperature sensor as published by the platform team. Implementers in other teams' code only provide
 * {@link #id()} and {@link #readCelsius()}. Everything else is a default method, so we could add it without
 * breaking them.
 *
 * <p>Task 39: read {@code tasks/39-default-methods-sensors.md} first.
 */
public interface TemperatureSensor {

    String id();

    double readCelsius();

    /** Fahrenheit is derived from Celsius unless the implementation has a native Fahrenheit reading. */
    default double readFahrenheit() {
        throw new UnsupportedOperationException("TODO: Task 39");
    }

    /** The boundary is inclusive: water at exactly 0.0 C counts as freezing. */
    default boolean isFreezing() {
        throw new UnsupportedOperationException("TODO: Task 39");
    }

    /**
     * Returns a sensor with the same id whose reading is this sensor's reading plus {@code offsetCelsius}.
     *
     * <p>The result reads through to this sensor on every call. A snapshot would silently stop following
     * the hardware after the first call.
     */
    default TemperatureSensor calibrated(double offsetCelsius) {
        throw new UnsupportedOperationException("TODO: Task 39");
    }

    /**
     * A sensor that always reports the same value. Useful for tests and for stubbing out hardware.
     *
     * @throws IllegalArgumentException if {@code id} is blank
     */
    static TemperatureSensor fixed(String id, double celsius) {
        throw new UnsupportedOperationException("TODO: Task 39");
    }
}
