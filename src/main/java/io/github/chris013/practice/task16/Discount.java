package io.github.chris013.practice.task16;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

/**
 * Given. What one promotion took off the basket.
 *
 * @param promotionName the promotion that was applied, as it should appear on the receipt
 * @param amount        the money taken off, always positive, with two decimals
 * @param claimedSkus   the products this discount used up; later line-level promotions must leave them alone
 */
public record Discount(String promotionName, BigDecimal amount, Set<Sku> claimedSkus) {

    public Discount {
        Objects.requireNonNull(promotionName, "promotionName");
        amount = Cents.of(amount, "amount");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("A discount must be positive, was " + amount);
        }
        claimedSkus = Set.copyOf(Objects.requireNonNull(claimedSkus, "claimedSkus"));
    }
}
