package io.github.chris013.practice.task16;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The state that flows through the {@link PromotionEngine}: the lines, the SKUs already used up by discounts,
 * and the discount so far.
 *
 * <p>Immutable. {@link #after(Discount)} returns a new basket and never changes this one. That is what makes the
 * pipeline easy to reason about: a promotion cannot change the basket under the feet of the promotions after it.
 */
public record Basket(List<BasketLine> lines, Set<Sku> claimedSkus, BigDecimal discountSoFar) {

    public Basket {
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        claimedSkus = Set.copyOf(Objects.requireNonNull(claimedSkus, "claimedSkus"));
        discountSoFar = Cents.of(discountSoFar, "discountSoFar");
        if (discountSoFar.signum() < 0) {
            throw new IllegalArgumentException("discountSoFar cannot be negative, was " + discountSoFar);
        }
    }

    /** A basket with no discounts and nothing claimed. */
    public static Basket of(List<BasketLine> lines) {
        return new Basket(lines, Set.of(), Cents.ZERO);
    }

    /** The price of all lines before any discount. An empty basket has a subtotal of 0.00. */
    public BigDecimal subtotal() {
        return lines.stream().map(BasketLine::total).reduce(Cents.ZERO, BigDecimal::add);
    }

    /** The subtotal minus the discounts applied so far. */
    public BigDecimal total() {
        return subtotal().subtract(discountSoFar);
    }

    /** The lines whose SKU is not claimed yet. Line-level promotions only look at these. */
    public List<BasketLine> unclaimedLines() {
        return lines.stream().filter(line -> !claimedSkus.contains(line.sku())).toList();
    }

    /** A new basket with the discount's amount added and its SKUs claimed. */
    public Basket after(Discount discount) {
        Objects.requireNonNull(discount, "discount");
        Set<Sku> claimed = Stream.concat(claimedSkus.stream(), discount.claimedSkus().stream())
                .collect(Collectors.toUnmodifiableSet());
        return new Basket(lines, claimed, discountSoFar.add(discount.amount()));
    }
}
