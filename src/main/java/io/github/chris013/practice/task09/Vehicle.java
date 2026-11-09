package io.github.chris013.practice.task09;

import java.util.Objects;

/**
 * The car that is being insured.
 *
 * @param model      free text, for the policy document only
 * @param horsepower must be positive
 * @param valueEuros market value in whole euros, must be positive
 * @param yearBuilt  model year, for the policy document only
 */
public record Vehicle(String model, int horsepower, int valueEuros, int yearBuilt) {

    public Vehicle {
        Objects.requireNonNull(model, "model");
        if (horsepower <= 0) {
            throw new IllegalArgumentException("horsepower must be positive: " + horsepower);
        }
        if (valueEuros <= 0) {
            throw new IllegalArgumentException("valueEuros must be positive: " + valueEuros);
        }
    }
}
