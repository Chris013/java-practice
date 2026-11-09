package io.github.chris013.practice.task20.domain;

import java.util.Optional;

/**
 * The port the loyalty context uses to look up customers. The implementation lives in another context
 * (the anti-corruption layer), so this interface must speak only domain language.
 */
public interface CustomerDirectory {

    /**
     * @return the customer, or empty when there is no such customer
     * @throws CustomerDirectoryUnavailableException when the directory cannot answer right now; retry later
     * @throws CorruptCustomerDataException when the stored data cannot be turned into a valid customer
     */
    Optional<Customer> findById(CustomerId id);
}
