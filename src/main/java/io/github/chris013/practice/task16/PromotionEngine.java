package io.github.chris013.practice.task16;

import java.util.List;

/**
 * Prices a basket by applying promotions one after another, in the order they were given. Each promotion sees the
 * basket as the earlier ones left it. Put line-level promotions first: a basket-level one such as "5 off from 50"
 * then sees the total after those discounts.
 *
 * <p>Task 16: read {@code tasks/16-promotion-engine.md} first.
 */
public final class PromotionEngine {

    public PromotionEngine(List<Promotion> promotionsInPriorityOrder) {
        throw new UnsupportedOperationException("TODO: Task 16");
    }

    /**
     * Applies every promotion that applies, in order. The total is the subtotal minus all discounts, and never
     * below 0.00.
     */
    public Receipt price(List<BasketLine> lines) {
        throw new UnsupportedOperationException("TODO: Task 16");
    }
}
