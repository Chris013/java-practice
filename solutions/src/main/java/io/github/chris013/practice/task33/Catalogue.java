package io.github.chris013.practice.task33;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An in-memory, read-only view over a set of products. One generic {@code find} replaces one method per query.
 */
public final class Catalogue {

    private final List<Product> products;

    /** Copies the collection. Later changes to the caller's collection do not show up here. */
    public Catalogue(Collection<Product> products) {
        this.products = List.copyOf(Objects.requireNonNull(products, "products"));
    }

    public List<Product> find(Predicate<? super Product> filter, Comparator<? super Product> order) {
        Objects.requireNonNull(order, "order");
        return matching(filter).sorted(order).toList();
    }

    /**
     * {@code Stream.min} keeps the earlier element when two are equal, so on a tie the product that comes
     * first in the catalogue wins.
     */
    public Optional<Product> findFirst(Predicate<? super Product> filter, Comparator<? super Product> order) {
        Objects.requireNonNull(order, "order");
        return matching(filter).min(order);
    }

    public <R> List<R> project(Predicate<? super Product> filter, Function<? super Product, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return matching(filter).<R>map(mapper).toList();
    }

    /**
     * Only categories with at least one match appear as keys. The map is in enum order and is unmodifiable.
     */
    public Map<Category, Long> countBy(Predicate<? super Product> filter) {
        Map<Category, Long> counts = matching(filter)
                .collect(Collectors.groupingBy(Product::category,
                        () -> new EnumMap<>(Category.class),
                        Collectors.counting()));
        return Collections.unmodifiableMap(counts);
    }

    private Stream<Product> matching(Predicate<? super Product> filter) {
        Objects.requireNonNull(filter, "filter");
        return products.stream().filter(filter);
    }
}
