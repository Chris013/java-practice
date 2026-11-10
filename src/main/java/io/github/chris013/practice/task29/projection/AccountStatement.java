package io.github.chris013.practice.task29.projection;

import io.github.chris013.practice.task29.domain.AccountEvent;
import java.util.List;

/**
 * A read model: the account statement, one line per event. It is built from the same events as {@code Account}
 * but has its own switch and its own running balance. It does not call into the aggregate: read models change for
 * reasons of their own, and they should not force a change on the write side.
 *
 * <p>Dates are taken in UTC so that the same history gives the same statement on every machine.
 *
 * <p>Task 29: read {@code tasks/29-event-sourced-account.md} first.
 */
public final class AccountStatement {

    private AccountStatement() {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    public static AccountStatement of(List<AccountEvent> history) {
        throw new UnsupportedOperationException("TODO: Task 29");
    }

    public List<StatementLine> lines() {
        throw new UnsupportedOperationException("TODO: Task 29");
    }
}
