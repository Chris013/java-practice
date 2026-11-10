package io.github.chris013.practice.task39;

import java.util.Objects;
import java.util.function.DoubleSupplier;

/** A sensor whose readings come from a supplier. Each call to {@link #readCelsius()} asks the supplier again. */
public final class SimulatedSensor extends AbstractTemperatureSensor {

    private final DoubleSupplier readings;

    public SimulatedSensor(String id, DoubleSupplier readings) {
        super(id);
        this.readings = Objects.requireNonNull(readings, "readings");
    }

    @Override
    public double readCelsius() {
        return readings.getAsDouble();
    }
}
