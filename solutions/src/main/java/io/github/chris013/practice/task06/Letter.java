package io.github.chris013.practice.task06;

/** A letter or large letter, up to 1000 g. */
public record Letter(int weightGrams) implements Shipment {

    static final int MAX_WEIGHT_GRAMS = 1000;

    public Letter {
        if (weightGrams < 1 || weightGrams > MAX_WEIGHT_GRAMS) {
            throw new IllegalArgumentException(
                    "A letter weighs 1 to %d g, not %d g. Heavier items are parcels.".formatted(MAX_WEIGHT_GRAMS, weightGrams));
        }
    }
}
