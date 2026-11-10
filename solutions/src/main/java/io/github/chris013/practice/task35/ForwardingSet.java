package io.github.chris013.practice.task35;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
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
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return delegate.toArray(a);
    }

    @Override
    public <T> T[] toArray(IntFunction<T[]> generator) {
        return delegate.toArray(generator);
    }

    @Override
    public boolean add(E e) {
        return delegate.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return delegate.addAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return delegate.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return delegate.removeAll(c);
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        return delegate.removeIf(filter);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        delegate.forEach(action);
    }

    @Override
    public Spliterator<E> spliterator() {
        return delegate.spliterator();
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
