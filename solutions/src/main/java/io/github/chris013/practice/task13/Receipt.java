package io.github.chris013.practice.task13;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The items of one shopping trip, with the totals the receipt has to print.
 *
 * <p>Task 13: read {@code tasks/13-receipt-enums.md} first.
 */
public final class Receipt {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final List<LineItem> items;

    private Receipt(List<LineItem> items) {
        this.items = items;
    }

    /**
     * Creates a receipt. The list is copied, so later changes to the caller's list do not affect the receipt.
     * An empty receipt is allowed: its totals are zero and it has no breakdown rows.
     */
    public static Receipt of(List<LineItem> items) {
        Objects.requireNonNull(items, "items");
        return new Receipt(List.copyOf(items));
    }

    /** The items, in the order they were scanned. The returned list cannot be changed. */
    public List<LineItem> items() {
        return items;
    }

    /** The sum of all line totals, with scale 2. The start value has scale 2 so an empty receipt prints 0.00. */
    public BigDecimal totalGross() {
        return items.stream().map(LineItem::totalGross).reduce(ZERO, BigDecimal::add);
    }

    /**
     * One {@link VatLine} per rate that occurs on the receipt, in enum order ({@code A} before {@code B}).
     *
     * <p>VAT is computed per rate on the sum of the gross amounts of that rate, rounded once. This is the method
     * the tax office expects on the receipt. Rounding each line on its own adds up to a different amount.
     */
    public Map<VatRate, VatLine> vatBreakdown() {
        Map<VatRate, BigDecimal> grossByRate = items.stream()
                .collect(Collectors.groupingBy(
                        item -> item.category().vatRate(),
                        () -> new EnumMap<>(VatRate.class),
                        Collectors.reducing(ZERO, LineItem::totalGross, BigDecimal::add)));

        Map<VatRate, VatLine> breakdown = new EnumMap<>(VatRate.class);
        for (Map.Entry<VatRate, BigDecimal> entry : grossByRate.entrySet()) {
            breakdown.put(entry.getKey(), vatLine(entry.getKey(), entry.getValue()));
        }
        return Collections.unmodifiableMap(breakdown);
    }

    /**
     * Splits a gross amount into net and VAT. The VAT part of a gross price is {@code gross x p / (100 + p)}, not
     * {@code gross x p / 100}, because the VAT is included in the price.
     */
    private static VatLine vatLine(VatRate rate, BigDecimal gross) {
        int percent = rate.percent();
        BigDecimal vat = gross.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100L + percent), 2, RoundingMode.HALF_UP);
        return new VatLine(rate, gross, gross.subtract(vat), vat);
    }

    /**
     * The highest minimum age of any item, or empty when no item is age-restricted. Items with age {@code 0}
     * are filtered out first, so an unrestricted receipt gives empty, not {@code 0}.
     */
    public OptionalInt minimumAgeToVerify() {
        return items.stream()
                .mapToInt(item -> item.category().minimumAge())
                .filter(age -> age > 0)
                .max();
    }

    /** The categories on the receipt, in enum order. The returned set cannot be changed. */
    public Set<Category> categories() {
        EnumSet<Category> categories = EnumSet.noneOf(Category.class);
        for (LineItem item : items) {
            categories.add(item.category());
        }
        return Collections.unmodifiableSet(categories);
    }
}
