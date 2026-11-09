package io.github.chris013.practice.task01;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;

/**
 * An amount of money in a specific currency.
 *
 * <p>Task 01: read {@code tasks/01-money-value-object.md} first. The public API below is fixed by the tests; how you
 * represent and implement it is up to you (you may turn this into a record).
 */
public final class Money {

    public static Money of(BigDecimal amount, Currency currency) {
        throw new UnsupportedOperationException("TODO: Task 01");
    }

    public static Money of(String amount, String currencyCode) {
        throw new UnsupportedOperationException("TODO: Task 01");
    }

    public static Money zero(Currency currency) {
        throw new UnsupportedOperationException("TODO: Task 01");
    }

    public BigDecimal amount() {
        throw new UnsupportedOperationException("TODO: Task 01");
    }

    public Currency currency() {
        throw new UnsupportedOperationException("TODO: Task 01");
    }

    public Money plus(Money other) {
        throw new UnsupportedOperationException("TODO: Task 01");
    }

    public Money minus(Money other) {
        throw new UnsupportedOperationException("TODO: Task 01");
    }

    public Money times(int quantity) {
        throw new UnsupportedOperationException("TODO: Task 01");
    }

    public Money multipliedBy(BigDecimal factor, RoundingMode rounding) {
        throw new UnsupportedOperationException("TODO: Task 01");
    }

    public List<Money> allocate(int... ratios) {
        throw new UnsupportedOperationException("TODO: Task 01");
    }

    public boolean isZero() {
        throw new UnsupportedOperationException("TODO: Task 01");
    }

    public boolean isNegative() {
        throw new UnsupportedOperationException("TODO: Task 01");
    }

    public boolean isGreaterThan(Money other) {
        throw new UnsupportedOperationException("TODO: Task 01");
    }
}
