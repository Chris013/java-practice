package io.github.chris013.practice.task25.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Given. What a customer owes for one billing event. */
public record Invoice(
        InvoiceId id,
        SubscriptionId subscriptionId,
        CustomerId customerId,
        LocalDate issuedOn,
        List<InvoiceLine> lines) {

    private static final BigDecimal NOTHING = new BigDecimal("0.00");

    public Invoice {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(subscriptionId, "subscriptionId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(issuedOn, "issuedOn");
        lines = List.copyOf(lines);
    }

    /**
     * The sum of the lines, never below zero. If credits are bigger than charges, the customer owes nothing.
     * Carrying the leftover credit to the next invoice is a stretch goal.
     */
    public BigDecimal total() {
        BigDecimal sum = lines.stream().map(InvoiceLine::amount).reduce(NOTHING, BigDecimal::add);
        return sum.signum() < 0 ? NOTHING : sum;
    }
}
