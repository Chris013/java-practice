package io.github.chris013.practice.task33;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An in-memory, read-only view over a set of products. One generic {@code find} replaces one method per query.
 *
 * <p>Task 33: read {@code tasks/33-catalogue-predicates.md} first.
 */
public final class Catalogue {

    public Catalogue(Collection<Product> products) {
        throw new UnsupportedOperationException("TODO: Task 33");
    }

    /** The matching products, sorted by {@code order}. The result is an immutable new list. */
    public List<Product> find(Predicate<? super Product> filter, Comparator<? super Product> order) {
        throw new UnsupportedOperationException("TODO: Task 33");
    }

    /** The first matching product in {@code order}, or empty if nothing matches. */
    public Optional<Product> findFirst(Predicate<? super Product> filter, Comparator<? super Product> order) {
        throw new UnsupportedOperationException("TODO: Task 33");
    }

    /** The mapped values of the matching products, in catalogue order. The result is immutable. */
    public <R> List<R> project(Predicate<? super Product> filter, Function<? super Product, ? extends R> mapper) {
        throw new UnsupportedOperationException("TODO: Task 33");
    }

    /** How many matching products there are per category. Categories without a match are left out. */
    public Map<Category, Long> countBy(Predicate<? super Product> filter) {
        throw new UnsupportedOperationException("TODO: Task 33");
    }
}
