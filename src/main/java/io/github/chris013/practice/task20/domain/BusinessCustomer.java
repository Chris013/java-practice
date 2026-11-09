package io.github.chris013.practice.task20.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

public record BusinessCustomer(
        CustomerId id,
        String companyName,
        Optional<String> contactPerson,
        Address address,
        CustomerStatus status,
        Optional<EmailAddress> email,
        BigDecimal creditLimit) implements Customer {

    public BusinessCustomer {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(companyName, "companyName");
        Objects.requireNonNull(contactPerson, "contactPerson");
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(creditLimit, "creditLimit");
        if (creditLimit.signum() < 0) {
            throw new IllegalArgumentException("A credit limit cannot be negative: " + creditLimit);
        }
    }
}
