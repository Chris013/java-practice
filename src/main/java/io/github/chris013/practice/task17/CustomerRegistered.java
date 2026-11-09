package io.github.chris013.practice.task17;

import java.util.Objects;

/** Given. A new customer finished the sign-up form. */
public record CustomerRegistered(String customerId, String email) implements DomainEvent {

    public CustomerRegistered {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(email, "email");
    }
}
