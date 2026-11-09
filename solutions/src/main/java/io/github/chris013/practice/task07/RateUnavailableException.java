package io.github.chris013.practice.task07;

import java.io.Serial;

/** The rate source could not deliver a rate right now (timeout, HTTP 503, rate limit, ...). Trying again may help. */
public final class RateUnavailableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public RateUnavailableException(CurrencyPair pair, String reason) {
        super("No rate for %s: %s".formatted(pair, reason));
    }
}
