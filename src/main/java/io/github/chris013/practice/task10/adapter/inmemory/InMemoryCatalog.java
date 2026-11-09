package io.github.chris013.practice.task10.adapter.inmemory;

import io.github.chris013.practice.task10.application.Catalog;
import io.github.chris013.practice.task10.domain.CopyId;

/**
 * Keeps the owned copies in memory. Demo adapter and test double.
 *
 * <p>Task 10: read {@code tasks/10-library-lending-hexagonal.md} first.
 */
public final class InMemoryCatalog implements Catalog {

    /** Registers copies the library owns. */
    public void add(CopyId... copyIds) {
        throw new UnsupportedOperationException("TODO: Task 10");
    }

    @Override
    public boolean exists(CopyId copyId) {
        throw new UnsupportedOperationException("TODO: Task 10");
    }
}
