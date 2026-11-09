package io.github.chris013.practice.task16;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Takes a percentage off every unclaimed line in one category, for example "10% off bakery".
 *
 * <p>The amount is computed once, on the sum of the category, and rounded once. Rounding each line separately
 * can end up a cent away from what the customer would expect for the whole category.
 */
public final class PercentOffCategory implements Promotion {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final String name;
    private final Category category;
    private final int percent;

    public PercentOffCategory(String name, Category category, int percent) {
        this.name = Objects.requireNonNull(name, "name");
        this.category = Objects.requireNonNull(category, "category");
        if (percent < 1 || percent > 100) {
            throw new IllegalArgumentException("percent must be from 1 to 100, was " + percent);
        }
        this.percent = percent;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Optional<Discount> evaluate(Basket basket) {
        List<BasketLine> matching = basket.unclaimedLines().stream()
                .filter(line -> line.category() == category)
                .toList();
        if (matching.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal eligible = matching.stream().map(BasketLine::total).reduce(Cents.ZERO, BigDecimal::add);
        BigDecimal amount = eligible.multiply(BigDecimal.valueOf(percent)).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        // A discount of zero is not a discount: Discount refuses it, and nothing is claimed.
        if (amount.signum() == 0) {
            return Optional.empty();
        }
        Set<Sku> claimed = matching.stream().map(BasketLine::sku).collect(Collectors.toUnmodifiableSet());
        return Optional.of(new Discount(name, amount, claimed));
    }
}
