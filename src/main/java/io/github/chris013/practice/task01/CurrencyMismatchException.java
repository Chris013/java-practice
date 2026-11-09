package io.github.chris013.practice.task01;

import java.io.Serial;
import java.util.Currency;

/** Thrown when two amounts in different currencies are combined, e.g. EUR 10.00 plus USD 5.00. */
public final class CurrencyMismatchException extends IllegalArgumentException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CurrencyMismatchException(Currency expected, Currency actual) {
        super("Cannot combine %s with %s".formatted(expected.getCurrencyCode(), actual.getCurrencyCode()));
    }
}
