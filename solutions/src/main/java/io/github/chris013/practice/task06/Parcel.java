package io.github.chris013.practice.task06;

import java.util.Objects;

public record Parcel(int weightGrams, Dimensions dimensions) implements Shipment {

    public Parcel {
        if (weightGrams < 1) {
            throw new IllegalArgumentException("A parcel must weigh at least 1 g, not %d g".formatted(weightGrams));
        }
        Objects.requireNonNull(dimensions, "dimensions");
    }

    /** What we charge for: the actual weight, or the volumetric weight if the parcel is light but bulky. */
    public int billableWeightGrams() {
        return Math.max(weightGrams, dimensions.volumetricWeightGrams());
    }
}
