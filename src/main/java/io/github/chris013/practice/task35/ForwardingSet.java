package io.github.chris013.practice.task35;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/**
 * A {@link Set} that forwards every call to another set, its delegate.
 *
 * <p>This is the base for wrappers such as {@link CountingSet}. A subclass overrides only the methods it
 * wants to change, and all other calls pass through to the delegate.
 *
 * <p>Task 35: read {@code tasks/35-counting-set-composition.md} first.
 *
 * <p>This class is not final, so it is designed for extension. It documents one rule: <b>no self-use</b>.
 * No method of this class calls another overridable method of this class. For example,
 * {@link #isEmpty()} asks the delegate, not {@code size()}, and {@link #addAll} hands the batch to the
 * delegate's {@code addAll} instead of looping over {@code add}. So an override in a subclass runs exactly
 * when the caller invokes that method, and never as a side effect of another method. Contrast
 * {@link java.util.AbstractCollection#addAll}, which calls {@code add} for each element. That is the bug
 * in {@link InheritedScanCounter}.
 *
 * <p>The wrapper is a view, not a copy. Changes through it show up in the delegate and the other way
 * round. {@code equals}, {@code hashCode} and {@code toString} are forwarded too, so a wrapper equals any
 * set with the same elements, in both directions.
 */
public class ForwardingSet<E> implements Set<E> {

    private final Set<E> delegate;

    /**
     * @param delegate the set that holds the elements; must not be {@code null}
     * @throws NullPointerException if {@code delegate} is {@code null}
     */
    public ForwardingSet(Set<E> delegate) {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    @Override
    public <T> T[] toArray(IntFunction<T[]> generator) {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    @Override
    public Spliterator<E> spliterator() {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    @Override
    public boolean equals(Object o) {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException("TODO: Task 35");
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException("TODO: Task 35");
    }
}
