package io.github.chris013.practice.task09;

/**
 * Builds a {@link Vehicle} for tests.
 *
 * <p>The defaults are a mid-range family car: 110 hp, worth 25,000 EUR. It triggers no
 * horsepower rule and no value rule.
 */
final class VehicleBuilder {

    private String model = "VW Golf";
    private int horsepower = 110;
    private int valueEuros = 25_000;
    private int yearBuilt = 2022;

    private VehicleBuilder() {
    }

    static VehicleBuilder aVehicle() {
        return new VehicleBuilder();
    }

    VehicleBuilder withHorsepower(int horsepower) {
        this.horsepower = horsepower;
        return this;
    }

    VehicleBuilder worth(int valueEuros) {
        this.valueEuros = valueEuros;
        return this;
    }

    Vehicle build() {
        return new Vehicle(model, horsepower, valueEuros, yearBuilt);
    }
}
