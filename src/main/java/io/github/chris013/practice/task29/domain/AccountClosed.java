package io.github.chris013.practice.task29.domain;

import java.time.Instant;
import java.util.Objects;

/** Given. The account was closed. Nothing can be done with it afterwards. */
public record AccountClosed(AccountId accountId, Instant occurredAt) implements AccountEvent {

    public AccountClosed {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
