package io.github.chris013.practice.task29.domain;

import java.time.Instant;
import java.util.Objects;

/** Given. Money came into the account. */
public record MoneyDeposited(AccountId accountId, Instant occurredAt, Money amount) implements AccountEvent {

    public MoneyDeposited {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(amount, "amount");
    }
}
