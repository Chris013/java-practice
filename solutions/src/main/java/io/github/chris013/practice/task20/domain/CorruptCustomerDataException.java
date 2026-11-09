package io.github.chris013.practice.task20.domain;

import java.util.Objects;

/**
 * The directory has a customer, but one field cannot be turned into a valid value. Retrying will not help:
 * someone has to fix the data. {@link #field()} is the legacy field name, such as {@code PLZ}.
 */
public final class CorruptCustomerDataException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final CustomerId customerId;
    private final String field;

    public CorruptCustomerDataException(CustomerId customerId, String field, String problem, Throwable cause) {
        super(messageFor(customerId, problem), cause);
        this.customerId = customerId;
        this.field = Objects.requireNonNull(field, "field");
    }

    public CustomerId customerId() {
        return customerId;
    }

    public String field() {
        return field;
    }

    private static String messageFor(CustomerId customerId, String problem) {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(problem, "problem");
        return "customer %s: %s".formatted(customerId.value(), problem);
    }
}
