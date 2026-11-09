package io.github.chris013.practice.task06;

/** A standard euro pallet, up to 1000 kg. Non-stackable pallets take up the space above them as well. */
public record Pallet(int weightKg, boolean stackable) implements Shipment {
    // TODO: Task 06, enforce the invariants
}
