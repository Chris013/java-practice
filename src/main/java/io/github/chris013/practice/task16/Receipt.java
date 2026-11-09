package io.github.chris013.practice.task16;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Given. The result of pricing a basket: what it cost, which discounts applied (in order) and what the customer pays.
 */
public record Receipt(BigDecimal subtotal, List<Discount> discounts, BigDecimal total) {

    public Receipt {
        Objects.requireNonNull(subtotal, "subtotal");
        discounts = List.copyOf(Objects.requireNonNull(discounts, "discounts"));
        Objects.requireNonNull(total, "total");
    }
}
