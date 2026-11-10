package io.github.chris013.practice.task29.application;

import io.github.chris013.practice.task29.domain.AccountId;
import java.io.Serial;

/**
 * Given. Another writer changed the stream since this one loaded it. The command that caused it is not lost: the
 * caller can reload the account and try again.
 */
public final class ConcurrencyException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ConcurrencyException(AccountId accountId, long expectedVersion, long actualVersion) {
        super("account " + accountId.value() + ": expected version " + expectedVersion
                + " but the stream is at " + actualVersion);
    }
}
