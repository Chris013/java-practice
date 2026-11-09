package io.github.chris013.practice.task16;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Prices a basket by applying promotions one after another, in the order they were given. Each promotion sees the
 * basket as the earlier ones left it. Put line-level promotions first: a basket-level one such as "5 off from 50"
 * then sees the total after those discounts.
 *
 * <p>The engine knows nothing about the kinds of promotion. It only calls {@link Promotion#evaluate}. That is why
 * adding a new promotion never touches this class.
 */
public final class PromotionEngine {

    private final List<Promotion> promotions;

    public PromotionEngine(List<Promotion> promotionsInPriorityOrder) {
        this.promotions = List.copyOf(Objects.requireNonNull(promotionsInPriorityOrder, "promotionsInPriorityOrder"));
    }

    /**
     * Applies every promotion that applies, in order. The total is the subtotal minus all discounts, and never
     * below 0.00.
     */
    public Receipt price(List<BasketLine> lines) {
        Basket basket = Basket.of(lines);
        List<Discount> applied = new ArrayList<>();
        for (Promotion promotion : promotions) {
            Optional<Discount> discount = promotion.evaluate(basket);
            if (discount.isPresent()) {
                applied.add(discount.get());
                basket = basket.after(discount.get());
            }
        }
        return new Receipt(basket.subtotal(), applied, basket.total().max(Cents.ZERO));
    }
}
