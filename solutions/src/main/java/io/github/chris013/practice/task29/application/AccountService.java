package io.github.chris013.practice.task29.application;

import io.github.chris013.practice.task29.domain.Account;
import io.github.chris013.practice.task29.domain.AccountEvent;
import io.github.chris013.practice.task29.domain.AccountId;
import io.github.chris013.practice.task29.domain.Money;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
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

    private final EventStore store;
    private final Clock clock;
    private final Supplier<AccountId> ids;

    public AccountService(EventStore store, Clock clock, Supplier<AccountId> ids) {
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.ids = Objects.requireNonNull(ids, "ids");
    }

    /** Opens a new account. A new stream must be created at version 0, so this never retries. */
    public AccountId open(String owner, Money overdraftLimit) {
        AccountId id = ids.get();
        Objects.requireNonNull(id, "id");
        Account account = Account.open(id, owner, overdraftLimit, clock.instant());
        store.append(id, 0, account.uncommittedEvents());
        account.markCommitted();
        return id;
    }

    public void deposit(AccountId id, Money amount) {
        Objects.requireNonNull(amount, "amount");
        execute(id, account -> account.deposit(amount, clock.instant()));
    }

    public void withdraw(AccountId id, Money amount) {
        Objects.requireNonNull(amount, "amount");
        execute(id, account -> account.withdraw(amount, clock.instant()));
    }

    public void changeOverdraftLimit(AccountId id, Money newLimit) {
        Objects.requireNonNull(newLimit, "newLimit");
        execute(id, account -> account.changeOverdraftLimit(newLimit, clock.instant()));
    }

    public void close(AccountId id) {
        execute(id, account -> account.close(clock.instant()));
    }

    public Money balanceOf(AccountId id) {
        return load(id).balance();
    }

    /**
     * Runs one command with optimistic concurrency. A business exception (insufficient funds, closed account, ...)
     * leaves this method at once and is never retried: retrying cannot make a refused withdrawal valid.
     */
    private void execute(AccountId id, Consumer<Account> command) {
        ConcurrencyException lastConflict = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            Account account = load(id);
            long versionLoaded = account.version();
            command.accept(account);
            try {
                store.append(id, versionLoaded, account.uncommittedEvents());
                account.markCommitted();
                return;
            } catch (ConcurrencyException conflict) {
                lastConflict = conflict;
            }
        }
        throw lastConflict;
    }

    private Account load(AccountId id) {
        Objects.requireNonNull(id, "id");
        List<AccountEvent> history = store.load(id);
        if (history.isEmpty()) {
            throw new IllegalArgumentException("unknown account " + id.value());
        }
        return Account.replay(history);
    }
}
