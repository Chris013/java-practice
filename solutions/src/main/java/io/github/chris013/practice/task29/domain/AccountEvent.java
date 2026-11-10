package io.github.chris013.practice.task29.domain;

import java.time.Instant;

/**
 * Given. A fact about an account that already happened. Events are named in the past tense, are never changed
 * after they are written, and are the only thing the event store keeps. The set is closed, so {@code switch}
 * over it needs no {@code default} branch.
 */
public sealed interface AccountEvent permits AccountOpened, MoneyDeposited, MoneyWithdrawn, OverdraftLimitChanged,
        AccountClosed {

    AccountId accountId();

    Instant occurredAt();
}
