package io.github.chris013.practice.task29.application;

import io.github.chris013.practice.task29.domain.AccountId;
import io.github.chris013.practice.task29.domain.Money;
import java.time.Clock;
import java.util.function.Supplier;

/**
 * Application service: the use cases for accounts. Each command loads the history, replays it, runs the command
 * against that state, and appends the new events with the version it loaded. If another writer got in between,
 * the store says so with a {@link ConcurrencyException}, and the whole cycle runs again on the newer state.
 *
 * <p>Task 29: read {@code tasks/29-event-sourced-account.md} first.
 */
public final class AccountService {

    /** Total attempts for one command, including the first one. */
    static final int MAX_ATTEMPTS = 3;

    public AccountService(EventStore store, Clock clock, Supplier<AccountId> ids) {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    /** Opens a new account. A new stream must be created at version 0, so this never retries. */
    public AccountId open(String owner, Money overdraftLimit) {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    public void deposit(AccountId id, Money amount) {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    public void withdraw(AccountId id, Money amount) {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    public void changeOverdraftLimit(AccountId id, Money newLimit) {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    public void close(AccountId id) {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    public Money balanceOf(AccountId id) {
        throw new UnsupportedOperationException("TODO: Task 29");
    }
}
