package io.github.chris013.practice.task32;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
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
        this.customers = Objects.requireNonNull(customers, "customers");
        this.pickupPoints = Objects.requireNonNull(pickupPoints, "pickupPoints");
        this.deliverableCountries = Set.copyOf(Objects.requireNonNull(deliverableCountries, "deliverableCountries"));
    }

    /** Empty when the customer is unknown or no address of theirs can receive a parcel. */
    public Optional<Address> resolve(CustomerId id) {
        Objects.requireNonNull(id, "id");
        return customers.find(id)
                .flatMap(customer -> preferredPickupAddress(customer)
                        .or(() -> customer.homeAddress().filter(this::isDeliverable)));
    }

    /**
     * @throws NoDeliveryAddressException when {@link #resolve(CustomerId)} is empty
     */
    public Address resolveOrThrow(CustomerId id) {
        return resolve(id).orElseThrow(() -> new NoDeliveryAddressException(id));
    }

    /** The city of the resolved address, or {@code "unknown"} when nothing resolves. */
    public String deliveryCity(CustomerId id) {
        return resolve(id).map(Address::city).orElse(UNKNOWN_CITY);
    }

    /** The resolved addresses in input order. Customers without an address are skipped. */
    public List<Address> resolveAll(Collection<CustomerId> ids) {
        Objects.requireNonNull(ids, "ids");
        return ids.stream()
                .map(this::resolve)
                .flatMap(Optional::stream)
                .toList();
    }

    /** Only a pickup point that exists, is open and is in a deliverable country counts. */
    private Optional<Address> preferredPickupAddress(Customer customer) {
        return customer.preferredPickupPointId()
                .flatMap(pickupPoints::find)
                .filter(PickupPoint::open)
                .map(PickupPoint::address)
                .filter(this::isDeliverable);
    }

    private boolean isDeliverable(Address address) {
        return deliverableCountries.contains(address.countryCode());
    }
}
