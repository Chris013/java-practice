package io.github.chris013.practice.task29.domain;

import java.time.Instant;
import java.util.List;

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

    private Account() {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    /** Opens a new account and records {@link AccountOpened}. The caller passes the clock reading as {@code now}. */
    public static Account open(AccountId id, String owner, Money overdraftLimit, Instant now) {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    /**
     * Rebuilds an account from its history. Nothing is uncommitted afterwards, and {@link #version()} equals the
     * number of events. The history is checked for shape only (one account, opened once, first), not for business
     * rules: an old history must load even if today's rules would refuse it.
     */
    public static Account replay(List<AccountEvent> history) {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    public void deposit(Money amount, Instant now) {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    /**
     * The balance may go down to minus the overdraft limit, and no further. The limit itself is allowed; one cent
     * beyond it is refused.
     */
    public void withdraw(Money amount, Instant now) {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    /** The new limit may not be below the current debt, or the account would be overdrawn beyond its own limit. */
    public void changeOverdraftLimit(Money newLimit, Instant now) {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    /** Only a settled account can be closed: the balance must be exactly zero. */
    public void close(Instant now) {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    public AccountId id() {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    public String owner() {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    public Money balance() {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    public Money overdraftLimit() {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    public boolean isClosed() {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    /** The number of events applied so far, committed and uncommitted together. Used as the expected version. */
    public long version() {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    /** The events recorded since the last {@link #markCommitted()}, as an unmodifiable copy. */
    public List<AccountEvent> uncommittedEvents() {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    /** Call after the events have been stored. Clears the list so the next save writes only new events. */
    public void markCommitted() {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    /** Records the event as a new fact: applies it to the state and adds it to the uncommitted list. */
    private void record(AccountEvent event) {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    /**
     * Changes the state to match the event. No validation here, on purpose: this runs for new events and for every
     * event in every old history. Adding a {@code default} branch would hide a new event type that nobody handled.
     */
    private void apply(AccountEvent event) {
        throw new UnsupportedOperationException("TODO: Task 29");
    }
}
