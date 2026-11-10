package io.github.chris013.practice.task29.domain;

import java.time.Instant;
import java.util.Objects;

/** Given. The overdraft limit was set to a new value. */
public record OverdraftLimitChanged(AccountId accountId, Instant occurredAt, Money newLimit) implements AccountEvent {

    public OverdraftLimitChanged {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(newLimit, "newLimit");
    }
}
