package io.github.chris013.practice.task29.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Given. An amount of euros with a fixed scale of two, so that {@code 12.5} and {@code 12.50} are the same money.
 * Negative amounts are allowed because a balance can be negative when the account is overdrawn.
 */
public record Money(BigDecimal amount) implements Comparable<Money> {

    private static final int SCALE = 2;

    public Money {
        Objects.requireNonNull(amount, "amount");
        if (amount.stripTrailingZeros().scale() > SCALE) {
            throw new IllegalArgumentException("money has at most 2 decimal places: " + amount);
        }
        amount = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public static Money euros(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public Money plus(Money other) {
        return new Money(amount.add(Objects.requireNonNull(other, "other").amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(Objects.requireNonNull(other, "other").amount));
    }

    public Money negate() {
        return new Money(amount.negate());
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isLessThan(Money other) {
        return compareTo(other) < 0;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(Objects.requireNonNull(other, "other").amount);
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " EUR";
    }
}
