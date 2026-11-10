package io.github.chris013.practice.task29.domain;

import java.io.Serial;

/** Given. A withdrawal would take the balance below minus the overdraft limit. */
public final class InsufficientFundsException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InsufficientFundsException(Money attempted, Money available) {
        super("cannot withdraw " + attempted + ": only " + available + " available");
    }
}
