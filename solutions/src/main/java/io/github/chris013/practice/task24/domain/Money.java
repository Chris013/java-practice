package io.github.chris013.practice.task24.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * An amount of euros, always rounded to cents.
 *
 * <p>Why a type and not a bare {@code BigDecimal}: the rounding rule lives in one place, the constructor. Every
 * amount that exists is already in cents, so nobody can forget to round at a call site.
 */
public record Money(BigDecimal amount) {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(amount, "amount");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    public Money times(int factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)));
    }

    /** {@code percent} of this amount, e.g. {@code percent(10)} for 10 %. Rounded half up to cents. */
    public Money percent(int percent) {
        return new Money(amount.multiply(BigDecimal.valueOf(percent)).movePointLeft(2));
    }

    public boolean isAtLeast(Money other) {
        return amount.compareTo(other.amount) >= 0;
    }
}
