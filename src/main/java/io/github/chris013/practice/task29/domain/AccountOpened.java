package io.github.chris013.practice.task29.domain;

import java.time.Instant;
import java.util.Objects;

/** Given. The first event of every stream. */
public record AccountOpened(AccountId accountId, Instant occurredAt, String owner, Money overdraftLimit)
        implements AccountEvent {

    public AccountOpened {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(overdraftLimit, "overdraftLimit");
    }
}
