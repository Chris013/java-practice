package io.github.chris013.practice.task33;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Reusable predicates for the catalogue search screen. Each factory returns a lambda that captures its arguments.
 * Combine them with {@link Predicate#and}, {@link Predicate#or}, {@link Predicate#negate}, {@link Predicate#not},
 * or with {@link #allOf} and {@link #anyOf} when the list of conditions comes from the user.
 */
public final class ProductFilters {

    private ProductFilters() {
        throw new AssertionError("no instances");
    }

    public static Predicate<Product> inCategory(Category category) {
        Objects.requireNonNull(category, "category");
        return product -> product.category() == category;
    }

    /**
     * Both bounds are inclusive. The comparison uses {@code compareTo}, so {@code 15} and {@code 15.00}
     * are the same bound.
     */
    public static Predicate<Product> priceBetween(BigDecimal minInclusive, BigDecimal maxInclusive) {
        Objects.requireNonNull(minInclusive, "minInclusive");
        Objects.requireNonNull(maxInclusive, "maxInclusive");
        if (minInclusive.compareTo(maxInclusive) > 0) {
            throw new IllegalArgumentException("min %s is above max %s".formatted(minInclusive, maxInclusive));
        }
        return product -> product.price().compareTo(minInclusive) >= 0
                && product.price().compareTo(maxInclusive) <= 0;
    }

    public static Predicate<Product> inStock() {
        return product -> product.stock() > 0;
    }

    /**
     * The text is lower-cased with {@link Locale#ROOT}, not the default locale. In Turkish, the default
     * locale maps {@code I} to a dotless letter, and a search for "ITEM" would silently fail.
     */
    public static Predicate<Product> nameContains(String text) {
        Objects.requireNonNull(text, "text");
        String needle = text.toLowerCase(Locale.ROOT);
        return product -> product.name().toLowerCase(Locale.ROOT).contains(needle);
    }

    public static Predicate<Product> heavierThan(int grams) {
        return product -> product.weightGrams() > grams;
    }

    /**
     * The list is copied, so the caller can change its own list later without changing this filter.
     * An empty list gives an empty conjunction, which is true for every product.
     */
    public static Predicate<Product> allOf(List<? extends Predicate<? super Product>> predicates) {
        List<Predicate<? super Product>> copy = List.copyOf(Objects.requireNonNull(predicates, "predicates"));
        return product -> copy.stream().allMatch(predicate -> predicate.test(product));
    }

    /** An empty list gives an empty disjunction, which is false for every product. */
    public static Predicate<Product> anyOf(List<? extends Predicate<? super Product>> predicates) {
        List<Predicate<? super Product>> copy = List.copyOf(Objects.requireNonNull(predicates, "predicates"));
        return product -> copy.stream().anyMatch(predicate -> predicate.test(product));
    }
}
