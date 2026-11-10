package io.github.chris013.practice.task32;

import java.util.Optional;

/** Looks up customer accounts. An unknown id is an empty result, not an exception. */
public interface CustomerDirectory {

    Optional<Customer> find(CustomerId id);
}
