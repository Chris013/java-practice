package io.github.chris013.practice.task13;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/**
 * The items of one shopping trip, with the totals the receipt has to print.
 *
 * <p>Task 13: read {@code tasks/13-receipt-enums.md} first.
 */
public final class Receipt {

    private final List<LineItem> items;

    private Receipt(List<LineItem> items) {
        this.items = items;
    }

    /**
     * Creates a receipt. The list is copied, so later changes to the caller's list do not affect the receipt.
     */
    public static Receipt of(List<LineItem> items) {
        throw new UnsupportedOperationException("TODO: Task 13");
    }

    /** The items, in the order they were scanned. The returned list cannot be changed. */
    public List<LineItem> items() {
        throw new UnsupportedOperationException("TODO: Task 13");
    }

    /** The sum of all line totals, with scale 2. */
    public BigDecimal totalGross() {
        throw new UnsupportedOperationException("TODO: Task 13");
    }

    /**
     * One {@link VatLine} per rate that occurs on the receipt, in enum order ({@code A} before {@code B}).
     *
     * <p>VAT is computed per rate on the sum of the gross amounts of that rate, rounded once. This is the method
     * the tax office expects on the receipt.
     */
    public Map<VatRate, VatLine> vatBreakdown() {
        throw new UnsupportedOperationException("TODO: Task 13");
    }

    /**
     * The highest minimum age of any item, or empty when no item is age-restricted.
     */
    public OptionalInt minimumAgeToVerify() {
        throw new UnsupportedOperationException("TODO: Task 13");
    }

    /** The categories on the receipt, in enum order. The returned set cannot be changed. */
    public Set<Category> categories() {
        throw new UnsupportedOperationException("TODO: Task 13");
    }
}
