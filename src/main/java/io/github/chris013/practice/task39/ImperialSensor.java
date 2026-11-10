package io.github.chris013.practice.task39;

import java.util.Objects;

/**
 * Given. A sensor that measures Fahrenheit natively (a US vendor's device). It overrides the default
 * {@code readFahrenheit()} with its raw value: the class method wins over the interface default.
 */
public final class ImperialSensor implements TemperatureSensor {

    private final String id;
    private final double fahrenheit;

    public ImperialSensor(String id, double fahrenheit) {
        this.id = Objects.requireNonNull(id, "id");
        this.fahrenheit = fahrenheit;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public double readFahrenheit() {
        return fahrenheit;
    }

    @Override
    public double readCelsius() {
        return (fahrenheit - 32) * 5 / 9;
    }
}
