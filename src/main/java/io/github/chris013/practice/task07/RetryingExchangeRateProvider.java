package io.github.chris013.practice.task07;

/** Tries again when the delegate reports a temporary failure. Task 07. */
public final class RetryingExchangeRateProvider implements ExchangeRateProvider {

    public RetryingExchangeRateProvider(ExchangeRateProvider delegate, int maxAttempts) {
        throw new UnsupportedOperationException("TODO: Task 07");
    }

    @Override
    public ExchangeRate rateFor(CurrencyPair pair) {
        throw new UnsupportedOperationException("TODO: Task 07");
    }
}
