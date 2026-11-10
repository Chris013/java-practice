package io.github.chris013.practice.task32;

import java.util.Objects;
import java.util.Optional;

/**
 * A customer account. The two optional facts are stored as plain nullable fields. Only the accessors wrap them
 * in {@link Optional}: an {@code Optional} field would add an extra object per customer and no extra safety.
 */
public final class Customer {

    private final CustomerId id;
    private final String name;
    private final Address homeAddress;
    private final String preferredPickupPointId;

    public Customer(CustomerId id, String name, Address homeAddress, String preferredPickupPointId) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.homeAddress = homeAddress;
        this.preferredPickupPointId = preferredPickupPointId;
    }

    public CustomerId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Optional<Address> homeAddress() {
        return Optional.ofNullable(homeAddress);
    }

    public Optional<String> preferredPickupPointId() {
        return Optional.ofNullable(preferredPickupPointId);
    }
}
