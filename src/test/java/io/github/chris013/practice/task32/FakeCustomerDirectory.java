package io.github.chris013.practice.task32;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** An in-memory directory that counts how often it was asked. */
final class FakeCustomerDirectory implements CustomerDirectory {

    private final Map<CustomerId, Customer> customers = new HashMap<>();
    private int lookups;

    FakeCustomerDirectory with(Customer customer) {
        customers.put(customer.id(), customer);
        return this;
    }

    int lookups() {
        return lookups;
    }

    @Override
    public Optional<Customer> find(CustomerId id) {
        lookups++;
        return Optional.ofNullable(customers.get(id));
    }
}
