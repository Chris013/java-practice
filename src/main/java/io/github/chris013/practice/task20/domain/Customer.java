package io.github.chris013.practice.task20.domain;

import java.math.BigDecimal;
import java.util.Optional;

/** A customer as the loyalty program sees it. Closed set: a private person or a business. */
public sealed interface Customer permits PrivateCustomer, BusinessCustomer {

    CustomerId id();

    Address address();

    CustomerStatus status();

    Optional<EmailAddress> email();

    BigDecimal creditLimit();
}
