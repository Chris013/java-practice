package io.github.chris013.practice.task33;

import java.util.Comparator;

/**
 * Reusable comparators for the catalogue search screen.
 *
 * <p>Task 33: read {@code tasks/33-catalogue-predicates.md} first.
 */
public final class ProductOrder {

    private ProductOrder() {
        throw new UnsupportedOperationException("TODO: Task 33");
    }

    /** Cheapest first. */
    public static Comparator<Product> byPrice() {
        throw new UnsupportedOperationException("TODO: Task 33");
    }

    /** Most items in stock first. */
    public static Comparator<Product> byStockDescending() {
        throw new UnsupportedOperationException("TODO: Task 33");
    }

    /** Category in enum order, then name ignoring case. */
    public static Comparator<Product> byCategoryThenName() {
        throw new UnsupportedOperationException("TODO: Task 33");
    }

    /** Price ascending, then weight ascending, then SKU, so that the order is total. */
    public static Comparator<Product> cheapestThenLightest() {
        throw new UnsupportedOperationException("TODO: Task 33");
    }
}
