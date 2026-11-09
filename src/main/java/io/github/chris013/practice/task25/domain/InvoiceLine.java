package io.github.chris013.practice.task25.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Given. One row on an invoice. A negative amount is a credit. Amounts always have two decimals. */
public record InvoiceLine(String description, BigDecimal amount) {

    public InvoiceLine {
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(amount, "amount");
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    }
}
