package io.github.chris013.practice.task16;

import java.util.Optional;

/**
 * Given. A pricing rule: the strategy the {@link PromotionEngine} plugs in.
 *
 * <p>This interface is open on purpose. Marketing adds new kinds of promotion, so nobody should have to edit a
 * closed list to add one. A promotion must not change the basket it is given: it looks at the basket and
 * returns a {@link Discount} or nothing.
 */
public interface Promotion {

    /** The name shown on the receipt when this promotion applies. */
    String name();

    /**
     * Decides whether this promotion applies to the basket as it is right now.
     *
     * @return the discount, or empty when the promotion does not apply
     */
    Optional<Discount> evaluate(Basket basket);
}
