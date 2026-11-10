package io.github.chris013.practice.task33;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Predicate;

/**
 * Reusable predicates for the catalogue search screen.
 *
 * <p>Task 33: read {@code tasks/33-catalogue-predicates.md} first.
 */
public final class ProductFilters {

    private ProductFilters() {
        throw new UnsupportedOperationException("TODO: Task 33");
    }

    /** Matches products whose category is {@code category}. */
    public static Predicate<Product> inCategory(Category category) {
        throw new UnsupportedOperationException("TODO: Task 33");
    }

    /** Matches prices from {@code minInclusive} to {@code maxInclusive}, both included. */
    public static Predicate<Product> priceBetween(BigDecimal minInclusive, BigDecimal maxInclusive) {
        throw new UnsupportedOperationException("TODO: Task 33");
    }

    /** Matches products with at least one item in stock. */
    public static Predicate<Product> inStock() {
        throw new UnsupportedOperationException("TODO: Task 33");
    }

    /** Matches names that contain {@code text}, ignoring case. */
    public static Predicate<Product> nameContains(String text) {
        throw new UnsupportedOperationException("TODO: Task 33");
    }

    /** Matches products strictly heavier than {@code grams}. */
    public static Predicate<Product> heavierThan(int grams) {
        throw new UnsupportedOperationException("TODO: Task 33");
    }

    /** Matches when every predicate matches. An empty list matches everything. */
    public static Predicate<Product> allOf(List<? extends Predicate<? super Product>> predicates) {
        throw new UnsupportedOperationException("TODO: Task 33");
    }

    /** Matches when at least one predicate matches. An empty list matches nothing. */
    public static Predicate<Product> anyOf(List<? extends Predicate<? super Product>> predicates) {
        throw new UnsupportedOperationException("TODO: Task 33");
    }
}
