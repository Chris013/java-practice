package io.github.chris013.practice.task29.projection;

import io.github.chris013.practice.task29.domain.Money;
import java.time.LocalDate;
import java.util.Objects;

/** Given. One row of an account statement: what happened, how much it moved the balance, and the balance after it. */
public record StatementLine(LocalDate date, String description, Money amount, Money balance) {

    public StatementLine {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(balance, "balance");
    }
}
