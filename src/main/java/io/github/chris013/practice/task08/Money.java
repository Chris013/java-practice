package io.github.chris013.practice.task08;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Given. A deliberately small euro-only money type (Task 01 builds the real one). */
public record Money(BigDecimal amount) implements Comparable<Money> {

    public static final Money ZERO = euros("0.00");

    public Money {
        Objects.requireNonNull(amount, "amount");
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static Money euros(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money times(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)));
    }

    public boolean isLessThan(Money other) {
        return compareTo(other) < 0;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }

    @Override
    public String toString() {
        return "EUR " + amount.toPlainString();
    }
}
