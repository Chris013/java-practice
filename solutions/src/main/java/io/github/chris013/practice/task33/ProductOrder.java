package io.github.chris013.practice.task33;

import java.util.Comparator;

/**
 * Reusable comparators for the catalogue search screen. Each one is a single-purpose comparator, and the
 * combined ones are built from small pieces with {@code thenComparing}.
 */
public final class ProductOrder {

    private ProductOrder() {
        throw new AssertionError("no instances");
    }

    public static Comparator<Product> byPrice() {
        return Comparator.comparing(Product::price);
    }

    /**
     * {@code reversed()} is applied to the single key comparator here. Applied to a whole chain, it would
     * also reverse the tie-breakers (see the Key decisions in the task).
     */
    public static Comparator<Product> byStockDescending() {
        return Comparator.comparingInt(Product::stock).reversed();
    }

    public static Comparator<Product> byCategoryThenName() {
        return Comparator.comparing(Product::category)
                .thenComparing(Product::name, String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * Ends with the SKU, so two different products never compare as equal. A sort by this comparator gives
     * the same result every time, whatever order the products came in.
     */
    public static Comparator<Product> cheapestThenLightest() {
        return Comparator.comparing(Product::price)
                .thenComparingInt(Product::weightGrams)
                .thenComparing(Product::sku);
    }
}
