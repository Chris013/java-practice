package io.github.chris013.practice.task32;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Decides where a parcel goes: the preferred pickup point if it is usable, otherwise the home address.
 *
 * <p>Task 32: read {@code tasks/32-optional-delivery-address.md} first.
 */
public final class DeliveryAddressResolver {

    private static final String UNKNOWN_CITY = "unknown";

    private final CustomerDirectory customers;
    private final PickupPointDirectory pickupPoints;
    private final Set<String> deliverableCountries;

    public DeliveryAddressResolver(
            CustomerDirectory customers, PickupPointDirectory pickupPoints, Set<String> deliverableCountries) {
        throw new UnsupportedOperationException("TODO: Task 32");
    }

    /** Empty when the customer is unknown or no address of theirs can receive a parcel. */
    public Optional<Address> resolve(CustomerId id) {
        throw new UnsupportedOperationException("TODO: Task 32");
    }

    /**
     * @throws NoDeliveryAddressException when {@link #resolve(CustomerId)} is empty
     */
    public Address resolveOrThrow(CustomerId id) {
        throw new UnsupportedOperationException("TODO: Task 32");
    }

    /** The city of the resolved address, or {@code "unknown"} when nothing resolves. */
    public String deliveryCity(CustomerId id) {
        throw new UnsupportedOperationException("TODO: Task 32");
    }

    /** The resolved addresses in input order. Customers without an address are skipped. */
    public List<Address> resolveAll(Collection<CustomerId> ids) {
        throw new UnsupportedOperationException("TODO: Task 32");
    }
}
