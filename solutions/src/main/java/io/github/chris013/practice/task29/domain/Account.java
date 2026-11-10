package io.github.chris013.practice.task29.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The account aggregate. Its balance is never stored: it is what you get when you replay the events.
 *
 * <p>Two kinds of methods, and the difference matters:
 * <ul>
 *   <li>Commands ({@link #deposit}, {@link #withdraw}, ...) check the rules against the current state. If the rules
 *       pass, they record a new event. If not, they throw and record nothing.</li>
 *   <li>{@link #apply} changes the state from an event. It never checks rules and never throws for business reasons,
 *       because an event is a fact that already happened. Replaying old events must always work, even after a rule
 *       has changed.</li>
 * </ul>
 *
 * <p>Task 29: read {@code tasks/29-event-sourced-account.md} first.
 */
public final class Account {

    private AccountId id;
    private String owner;
    private Money overdraftLimit;
    private Money balance = Money.zero();
    private boolean closed;
    private long version;
    private final List<AccountEvent> uncommitted = new ArrayList<>();

    private Account() {
    }

    /** Opens a new account and records {@link AccountOpened}. The caller passes the clock reading as {@code now}. */
    public static Account open(AccountId id, String owner, Money overdraftLimit, Instant now) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(overdraftLimit, "overdraftLimit");
        Objects.requireNonNull(now, "now");
        if (owner.isBlank()) {
            throw new IllegalArgumentException("owner must not be blank");
        }
        requireNonNegative(overdraftLimit, "overdraft limit");

        Account account = new Account();
        account.record(new AccountOpened(id, now, owner, overdraftLimit));
        return account;
    }

    /**
     * Rebuilds an account from its history. Nothing is uncommitted afterwards, and {@link #version()} equals the
     * number of events. The history is checked for shape only (one account, opened once, first), not for business
     * rules: an old history must load even if today's rules would refuse it.
     */
    public static Account replay(List<AccountEvent> history) {
        Objects.requireNonNull(history, "history");
        if (history.isEmpty()) {
            throw new IllegalArgumentException("history must not be empty");
        }
        AccountEvent first = history.getFirst();
        if (!(first instanceof AccountOpened)) {
            throw new IllegalArgumentException(
                    "history must start with AccountOpened, but starts with " + first.getClass().getSimpleName());
        }
        Account account = new Account();
        for (AccountEvent event : history) {
            if (!event.accountId().equals(first.accountId())) {
                throw new IllegalArgumentException("event for account " + event.accountId().value()
                        + " is in the history of account " + first.accountId().value());
            }
            if (event instanceof AccountOpened && account.version > 0) {
                throw new IllegalArgumentException("history contains a second AccountOpened");
            }
            account.apply(event);
        }
        return account;
    }

    public void deposit(Money amount, Instant now) {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(now, "now");
        requirePositive(amount, "deposit amount");
        requireOpen();
        record(new MoneyDeposited(id, now, amount));
    }

    /**
     * The balance may go down to minus the overdraft limit, and no further. The limit itself is allowed; one cent
     * beyond it is refused.
     */
    public void withdraw(Money amount, Instant now) {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(now, "now");
        requirePositive(amount, "withdrawal amount");
        requireOpen();
        Money lowestAllowed = overdraftLimit.negate();
        if (balance.minus(amount).isLessThan(lowestAllowed)) {
            throw new InsufficientFundsException(amount, balance.plus(overdraftLimit));
        }
        record(new MoneyWithdrawn(id, now, amount));
    }

    /** The new limit may not be below the current debt, or the account would be overdrawn beyond its own limit. */
    public void changeOverdraftLimit(Money newLimit, Instant now) {
        Objects.requireNonNull(newLimit, "newLimit");
        Objects.requireNonNull(now, "now");
        requireNonNegative(newLimit, "overdraft limit");
        requireOpen();
        Money debt = balance.isNegative() ? balance.negate() : Money.zero();
        if (newLimit.isLessThan(debt)) {
            throw new IllegalStateException(
                    "overdraft limit " + newLimit + " is below the current debt of " + debt);
        }
        record(new OverdraftLimitChanged(id, now, newLimit));
    }

    /** Only a settled account can be closed: the balance must be exactly zero. */
    public void close(Instant now) {
        Objects.requireNonNull(now, "now");
        requireOpen();
        if (balance.compareTo(Money.zero()) != 0) {
            throw new IllegalStateException(
                    "cannot close account " + id.value() + " while its balance is " + balance);
        }
        record(new AccountClosed(id, now));
    }

    public AccountId id() {
        return id;
    }

    public String owner() {
        return owner;
    }

    public Money balance() {
        return balance;
    }

    public Money overdraftLimit() {
        return overdraftLimit;
    }

    public boolean isClosed() {
        return closed;
    }

    /** The number of events applied so far, committed and uncommitted together. Used as the expected version. */
    public long version() {
        return version;
    }

    public List<AccountEvent> uncommittedEvents() {
        return List.copyOf(uncommitted);
    }

    /** Call after the events have been stored. Clears the list so the next save writes only new events. */
    public void markCommitted() {
        uncommitted.clear();
    }

    private void record(AccountEvent event) {
        apply(event);
        uncommitted.add(event);
    }

    /**
     * Changes the state to match the event. No validation here, on purpose: this runs for new events and for every
     * event in every old history. Adding a {@code default} branch would hide a new event type that nobody handled.
     */
    private void apply(AccountEvent event) {
        switch (event) {
            case AccountOpened opened -> {
                id = opened.accountId();
                owner = opened.owner();
                overdraftLimit = opened.overdraftLimit();
            }
            case MoneyDeposited deposited -> balance = balance.plus(deposited.amount());
            case MoneyWithdrawn withdrawn -> balance = balance.minus(withdrawn.amount());
            case OverdraftLimitChanged changed -> overdraftLimit = changed.newLimit();
            case AccountClosed accountClosed -> closed = true;
        }
        version++;
    }

    private void requireOpen() {
        if (closed) {
            throw new AccountClosedException(id);
        }
    }

    private static void requirePositive(Money amount, String what) {
        if (amount.compareTo(Money.zero()) <= 0) {
            throw new IllegalArgumentException(what + " must be positive, was " + amount);
        }
    }

    private static void requireNonNegative(Money amount, String what) {
        if (amount.isNegative()) {
            throw new IllegalArgumentException(what + " must not be negative, was " + amount);
        }
    }
}
