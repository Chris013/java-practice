package io.github.chris013.practice.task16;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A basket-level promotion: "5 EUR off from 50 EUR". It looks at the total as it is when the promotion is
 * evaluated, which includes the discounts of earlier promotions. It claims no SKUs, because it does not use up
 * any particular product.
 */
public final class SpendAndSave implements Promotion {

    private final String name;
    private final BigDecimal threshold;
    private final BigDecimal amountOff;

    public SpendAndSave(String name, BigDecimal threshold, BigDecimal amountOff) {
        this.name = Objects.requireNonNull(name, "name");
        this.threshold = Cents.of(Objects.requireNonNull(threshold, "threshold"), "threshold");
        if (this.threshold.signum() < 0) {
            throw new IllegalArgumentException("threshold cannot be negative, was " + threshold);
        }
        this.amountOff = Cents.of(Objects.requireNonNull(amountOff, "amountOff"), "amountOff");
        if (this.amountOff.signum() <= 0) {
            throw new IllegalArgumentException("amountOff must be positive, was " + amountOff);
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Optional<Discount> evaluate(Basket basket) {
        BigDecimal total = basket.total();
        if (total.compareTo(threshold) < 0) {
            return Optional.empty();
        }
        // Never take more than the customer still has to pay.
        BigDecimal amount = amountOff.min(total);
        if (amount.signum() <= 0) {
            return Optional.empty();
        }
        return Optional.of(new Discount(name, amount, Set.of()));
    }
}
