package io.github.chris013.practice.task29.adapter.inmemory;

import io.github.chris013.practice.task29.application.ConcurrencyException;
import io.github.chris013.practice.task29.application.EventStore;
import io.github.chris013.practice.task29.domain.AccountEvent;
import io.github.chris013.practice.task29.domain.AccountId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps every stream in a map, for tests and demos. The version check and the write for one stream happen inside
 * {@link Map#compute}, which holds that stream's bin lock. So two writers with the same expected version cannot both
 * pass the check. Stored lists are never modified; each append stores a new immutable list.
 */
public final class InMemoryEventStore implements EventStore {

    private final Map<AccountId, List<AccountEvent>> streams = new ConcurrentHashMap<>();

    @Override
    public List<AccountEvent> load(AccountId id) {
        Objects.requireNonNull(id, "id");
        return streams.getOrDefault(id, List.of());
    }

    @Override
    public void append(AccountId id, long expectedVersion, List<AccountEvent> events) {
        Objects.requireNonNull(id, "id");
        List<AccountEvent> toAppend = List.copyOf(events);
        streams.compute(id, (key, existing) -> {
            List<AccountEvent> current = existing == null ? List.of() : existing;
            if (current.size() != expectedVersion) {
                throw new ConcurrencyException(id, expectedVersion, current.size());
            }
            List<AccountEvent> next = new ArrayList<>(current.size() + toAppend.size());
            next.addAll(current);
            next.addAll(toAppend);
            return List.copyOf(next);
        });
    }
}
