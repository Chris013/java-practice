package io.github.chris013.practice.task16;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * A basket-level promotion: "5 EUR off from 50 EUR". It looks at the total as it is when the promotion is
 * evaluated, which includes the discounts of earlier promotions.
 *
 * <p>Task 16: read {@code tasks/16-promotion-engine.md} first.
 */
public final class SpendAndSave implements Promotion {

    public SpendAndSave(String name, BigDecimal threshold, BigDecimal amountOff) {
        throw new UnsupportedOperationException("TODO: Task 16");
    }

    @Override
    public String name() {
        throw new UnsupportedOperationException("TODO: Task 16");
    }

    @Override
    public Optional<Discount> evaluate(Basket basket) {
        throw new UnsupportedOperationException("TODO: Task 16");
    }
}
