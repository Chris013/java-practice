package io.github.chris013.practice.task07;

import java.util.Objects;

/**
 * Tries again when the delegate reports a temporary failure ({@link RateUnavailableException}). Other exceptions
 * mean "retrying won't help" and are passed through immediately. If every attempt fails, the first failure is
 * thrown with the later ones attached as suppressed exceptions, so nothing gets lost in the logs.
 */
public final class RetryingExchangeRateProvider implements ExchangeRateProvider {

    private final ExchangeRateProvider delegate;
    private final int maxAttempts;

    public RetryingExchangeRateProvider(ExchangeRateProvider delegate, int maxAttempts) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1, was " + maxAttempts);
        }
        this.maxAttempts = maxAttempts;
    }

    @Override
    public ExchangeRate rateFor(CurrencyPair pair) {
        RateUnavailableException firstFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return delegate.rateFor(pair);
            } catch (RateUnavailableException failure) {
                if (firstFailure == null) {
                    firstFailure = failure;
                } else {
                    firstFailure.addSuppressed(failure);
                }
            }
        }
        throw firstFailure;
    }
}
