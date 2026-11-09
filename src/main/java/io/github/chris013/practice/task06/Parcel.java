package io.github.chris013.practice.task06;

public record Parcel(int weightGrams, Dimensions dimensions) implements Shipment {
    // TODO: Task 06, enforce the invariants

    /** The weight we bill: the actual weight or the volumetric weight, whichever is higher. */
    public int billableWeightGrams() {
        throw new UnsupportedOperationException("TODO: Task 06");
    }
}
