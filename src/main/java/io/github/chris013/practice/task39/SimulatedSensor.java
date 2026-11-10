package io.github.chris013.practice.task39;

import java.util.function.DoubleSupplier;

/** A sensor whose readings come from a supplier. Each call to {@link #readCelsius()} asks the supplier again. */
public final class SimulatedSensor extends AbstractTemperatureSensor {

    public SimulatedSensor(String id, DoubleSupplier readings) {
        super(id);
        throw new UnsupportedOperationException("TODO: Task 39");
    }

    @Override
    public double readCelsius() {
        throw new UnsupportedOperationException("TODO: Task 39");
    }
}
