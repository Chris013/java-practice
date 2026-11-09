package io.github.chris013.practice.task16;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The state that flows through the {@link PromotionEngine}: the lines, the SKUs already used up by discounts,
 * and the discount so far. Immutable. {@link #after(Discount)} returns a new basket and never changes this one.
 *
 * <p>Task 16: read {@code tasks/16-promotion-engine.md} first.
 */
public record Basket(List<BasketLine> lines, Set<Sku> claimedSkus, BigDecimal discountSoFar) {

    public Basket {
        throw new UnsupportedOperationException("TODO: Task 16");
    }

    /** A basket with no discounts and nothing claimed. */
    public static Basket of(List<BasketLine> lines) {
        throw new UnsupportedOperationException("TODO: Task 16");
    }

    /** The price of all lines before any discount. An empty basket has a subtotal of 0.00. */
    public BigDecimal subtotal() {
        throw new UnsupportedOperationException("TODO: Task 16");
    }

    /** The subtotal minus the discounts applied so far. */
    public BigDecimal total() {
        throw new UnsupportedOperationException("TODO: Task 16");
    }

    /** The lines whose SKU is not claimed yet. Line-level promotions only look at these. */
    public List<BasketLine> unclaimedLines() {
        throw new UnsupportedOperationException("TODO: Task 16");
    }

    /** A new basket with the discount's amount added and its SKUs claimed. */
    public Basket after(Discount discount) {
        throw new UnsupportedOperationException("TODO: Task 16");
    }
}
