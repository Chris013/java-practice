package io.github.chris013.practice.task29.domain;

import java.time.Instant;
import java.util.Objects;

/** Given. Money left the account. */
public record MoneyWithdrawn(AccountId accountId, Instant occurredAt, Money amount) implements AccountEvent {

    public MoneyWithdrawn {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(amount, "amount");
    }
}
