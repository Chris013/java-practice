package io.github.chris013.practice.task29;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.chris013.practice.task29.domain.AccountClosed;
import io.github.chris013.practice.task29.domain.AccountEvent;
import io.github.chris013.practice.task29.domain.AccountId;
import io.github.chris013.practice.task29.domain.AccountOpened;
import io.github.chris013.practice.task29.domain.Money;
import io.github.chris013.practice.task29.domain.MoneyDeposited;
import io.github.chris013.practice.task29.domain.MoneyWithdrawn;
import io.github.chris013.practice.task29.domain.OverdraftLimitChanged;
import io.github.chris013.practice.task29.projection.AccountStatement;
import io.github.chris013.practice.task29.projection.StatementLine;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AccountStatementTest {

    private static final AccountId ID = new AccountId("acc-1");

    @Test
    void turns_a_scripted_history_into_exact_statement_lines() {
        List<AccountEvent> history = List.of(
                new AccountOpened(ID, Instant.parse("2026-03-01T09:00:00Z"), "Anna", Money.euros("500.00")),
                new MoneyDeposited(ID, Instant.parse("2026-03-02T10:00:00Z"), Money.euros("200.00")),
                new MoneyWithdrawn(ID, Instant.parse("2026-03-03T23:30:00Z"), Money.euros("350.00")),
                new OverdraftLimitChanged(ID, Instant.parse("2026-03-04T08:00:00Z"), Money.euros("800.00")),
                new MoneyDeposited(ID, Instant.parse("2026-03-05T12:00:00Z"), Money.euros("150.00")),
                new AccountClosed(ID, Instant.parse("2026-03-05T12:05:00Z")));

        List<StatementLine> lines = AccountStatement.of(history).lines();

        assertThat(lines).containsExactly(
                line("2026-03-01", "Account opened", "0.00", "0.00"),
                line("2026-03-02", "Deposit", "200.00", "200.00"),
                line("2026-03-03", "Withdrawal", "-350.00", "-150.00"),
                line("2026-03-04", "Overdraft limit set to 800.00 EUR", "0.00", "-150.00"),
                line("2026-03-05", "Deposit", "150.00", "0.00"),
                line("2026-03-05", "Account closed", "0.00", "0.00"));
    }

    @Test
    void takes_the_date_in_utc() {
        // 23:30 UTC on 31 March is already 1 April in Berlin. The statement must say 31 March for everyone.
        List<AccountEvent> history = List.of(
                new AccountOpened(ID, Instant.parse("2026-03-31T23:30:00Z"), "Anna", Money.zero()));

        assertThat(AccountStatement.of(history).lines()).extracting(StatementLine::date)
                .containsExactly(LocalDate.of(2026, 3, 31));
    }

    @Test
    void an_empty_history_has_no_lines() {
        assertThat(AccountStatement.of(List.of()).lines()).isEmpty();
    }

    private static StatementLine line(String date, String description, String amount, String balance) {
        return new StatementLine(
                LocalDate.parse(date), description, Money.euros(amount), Money.euros(balance));
    }
}
