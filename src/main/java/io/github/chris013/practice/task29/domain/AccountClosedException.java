package io.github.chris013.practice.task29.domain;

import java.io.Serial;

/** Given. A command was sent to an account that is already closed. */
public final class AccountClosedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public AccountClosedException(AccountId accountId) {
        super("account " + accountId.value() + " is closed");
    }
}
