package io.github.chris013.practice.task29.adapter.inmemory;

import io.github.chris013.practice.task29.application.EventStore;
import io.github.chris013.practice.task29.domain.AccountEvent;
import io.github.chris013.practice.task29.domain.AccountId;
import java.util.List;

/**
 * Keeps every stream in a map, for tests and demos. The version check and the write for one stream must be one
 * atomic step, or two writers with the same expected version can both succeed. Link: task 18.
 *
 * <p>Task 29: read {@code tasks/29-event-sourced-account.md} first.
 */
public final class InMemoryEventStore implements EventStore {

    public InMemoryEventStore() {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    @Override
    public List<AccountEvent> load(AccountId id) {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    @Override
    public void append(AccountId id, long expectedVersion, List<AccountEvent> events) {
        throw new UnsupportedOperationException("TODO: Task 29");
    }
}
