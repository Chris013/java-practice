package io.github.chris013.practice.task06;

/** Something a customer wants us to ship. Task 06: read {@code tasks/06-shipping-sealed-types.md} first. */
public sealed interface Shipment permits Letter, Parcel, Pallet {}
