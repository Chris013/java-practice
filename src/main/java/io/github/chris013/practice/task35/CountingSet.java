package io.github.chris013.practice.task35;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A set of distinct items that also counts every attempted add, duplicates included.
 *
 * <p>{@link #size()} is the number of distinct items, inherited from {@link ForwardingSet}. Use
 * {@link #addAttempts()} for the number of scans received. The counting happens here, in the wrapper, and
 * the delegate is only asked to store the items. Because {@link ForwardingSet#addAll} hands the whole batch
 * to the delegate and never calls {@link #add}, each element of a batch is counted exactly once.
 *
 * <p>Task 35: read {@code tasks/35-counting-set-composition.md} first.
 *
 * <p>The class is final. Counting is the whole contract, and a subclass could change what an "attempt"
 * means and break the dashboard numbers without anyone noticing.
 */
public final class CountingSet<E> extends ForwardingSet<E> {

    private int attempts;

    /**
     * @param delegate the set that stores the items; must not be {@code null}
     * @throws NullPointerException if {@code delegate} is {@code null}
     */
    public CountingSet(Set<E> delegate) {
        super(delegate);
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    /** A counting set over a new, empty {@link HashSet}. */
    public static <E> CountingSet<E> overHashSet() {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    @Override
    public boolean add(E element) {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    /**
     * Counts one attempt per element of {@code elements}, duplicates included. The count is taken from
     * the argument's size before the delegate runs, so a batch that fails half way is still counted in full.
     *
     * @throws NullPointerException if {@code elements} is null; nothing is counted then
     */
    @Override
    public boolean addAll(Collection<? extends E> elements) {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    /** Every attempted add so far, including adds of items that were already in the set. */
    public int addAttempts() {
        throw new UnsupportedOperationException("TODO: Task 35");
    }
}
