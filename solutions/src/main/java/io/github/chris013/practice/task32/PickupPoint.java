package io.github.chris013.practice.task32;

import java.util.Objects;

/** A parcel shop or locker where a customer can collect a parcel. */
public record PickupPoint(String id, Address address, boolean open) {

    public PickupPoint {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(address, "address");
    }
}
