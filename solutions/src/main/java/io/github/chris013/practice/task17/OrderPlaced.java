package io.github.chris013.practice.task17;

import java.math.BigDecimal;
import java.util.Objects;

/** Given. A customer placed an order. The total is money, so it is a {@link BigDecimal}. */
public record OrderPlaced(String orderId, String customerId, BigDecimal total) implements DomainEvent {

    public OrderPlaced {
        Objects.requireNonNull(orderId, "orderId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(total, "total");
    }
}
