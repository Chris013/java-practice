package io.github.chris013.practice.task06;

/** A standard euro pallet, up to 1000 kg. Non-stackable pallets take up the space above them as well. */
public record Pallet(int weightKg, boolean stackable) implements Shipment {

    static final int MAX_WEIGHT_KG = 1000;

    public Pallet {
        if (weightKg < 1 || weightKg > MAX_WEIGHT_KG) {
            throw new IllegalArgumentException("A pallet weighs 1 to %d kg, not %d kg".formatted(MAX_WEIGHT_KG, weightKg));
        }
    }
}
