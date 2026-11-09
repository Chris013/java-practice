package io.github.chris013.practice.task16;

import java.util.Optional;

/**
 * Takes a percentage off every unclaimed line in one category, for example "10% off bakery".
 *
 * <p>Task 16: read {@code tasks/16-promotion-engine.md} first.
 */
public final class PercentOffCategory implements Promotion {

    public PercentOffCategory(String name, Category category, int percent) {
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
