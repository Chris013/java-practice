package io.github.chris013.practice.task29.projection;

import io.github.chris013.practice.task29.domain.AccountClosed;
import io.github.chris013.practice.task29.domain.AccountEvent;
import io.github.chris013.practice.task29.domain.AccountOpened;
import io.github.chris013.practice.task29.domain.Money;
import io.github.chris013.practice.task29.domain.MoneyDeposited;
import io.github.chris013.practice.task29.domain.MoneyWithdrawn;
import io.github.chris013.practice.task29.domain.OverdraftLimitChanged;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A read model: the account statement, one line per event. It is built from the same events as {@code Account}
 * but has its own switch and its own running balance. It does not call into the aggregate. Read models change for
 * reasons of their own (a new column, a new report), and they should not force a change in the write side.
 *
 * <p>Dates are taken in UTC so that the same history gives the same statement on every machine.
 */
public final class AccountStatement {

    private final List<StatementLine> lines;

    private AccountStatement(List<StatementLine> lines) {
        this.lines = lines;
    }

    public static AccountStatement of(List<AccountEvent> history) {
        Objects.requireNonNull(history, "history");
        List<StatementLine> lines = new ArrayList<>(history.size());
        Money balance = Money.zero();
        for (AccountEvent event : history) {
            LocalDate date = LocalDate.ofInstant(event.occurredAt(), ZoneOffset.UTC);
            switch (event) {
                case AccountOpened _ ->
                        lines.add(new StatementLine(date, "Account opened", Money.zero(), balance));
                case MoneyDeposited deposited -> {
                    balance = balance.plus(deposited.amount());
                    lines.add(new StatementLine(date, "Deposit", deposited.amount(), balance));
                }
                case MoneyWithdrawn withdrawn -> {
                    balance = balance.minus(withdrawn.amount());
                    lines.add(new StatementLine(date, "Withdrawal", withdrawn.amount().negate(), balance));
                }
                case OverdraftLimitChanged changed ->
                        lines.add(new StatementLine(
                                date, "Overdraft limit set to " + changed.newLimit(), Money.zero(), balance));
                case AccountClosed _ ->
                        lines.add(new StatementLine(date, "Account closed", Money.zero(), balance));
            }
        }
        return new AccountStatement(List.copyOf(lines));
    }

    public List<StatementLine> lines() {
        return lines;
    }
}
