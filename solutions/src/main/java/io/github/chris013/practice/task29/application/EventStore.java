package io.github.chris013.practice.task29.application;

import io.github.chris013.practice.task29.domain.AccountEvent;
import io.github.chris013.practice.task29.domain.AccountId;
import java.util.List;

/**
 * Given. Outbound port: where the event streams live. The one operation that needs care is {@link #append}: the
 * version check and the write must happen as one atomic step, or two writers can both pass the check.
 */
public interface EventStore {

    /** The full history of the stream, oldest first. An unknown stream gives an empty list. */
    List<AccountEvent> load(AccountId id);

    /**
     * Appends events to the stream, but only if the stream currently has exactly {@code expectedVersion} events.
     *
     * @throws ConcurrencyException if another writer appended first
     */
    void append(AccountId id, long expectedVersion, List<AccountEvent> events);
}
