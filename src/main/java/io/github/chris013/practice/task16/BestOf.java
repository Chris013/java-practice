package io.github.chris013.practice.task16;

import java.util.List;
import java.util.Optional;

/**
 * A composite promotion: "the better of two offers". Every option is evaluated against the same basket and only
 * the best discount is used. A {@code BestOf} can contain another {@code BestOf}.
 *
 * <p>Task 16: read {@code tasks/16-promotion-engine.md} first.
 */
public final class BestOf implements Promotion {

    public BestOf(String name, List<Promotion> options) {
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
