package io.github.chris013.practice.task20.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public record PrivateCustomer(
        CustomerId id,
        String firstName,
        String lastName,
        Optional<LocalDate> birthDate,
        Address address,
        CustomerStatus status,
        Optional<EmailAddress> email,
        BigDecimal creditLimit) implements Customer {

    public PrivateCustomer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(firstName, "firstName");
        Objects.requireNonNull(lastName, "lastName");
        Objects.requireNonNull(birthDate, "birthDate");
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(creditLimit, "creditLimit");
        if (creditLimit.signum() < 0) {
            throw new IllegalArgumentException("A credit limit cannot be negative: " + creditLimit);
        }
    }
}
