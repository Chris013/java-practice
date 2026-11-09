package io.github.chris013.practice.task07;

/** Asks a secondary source when the primary one can't deliver. Task 07. */
public final class FallbackExchangeRateProvider implements ExchangeRateProvider {

    public FallbackExchangeRateProvider(ExchangeRateProvider primary, ExchangeRateProvider secondary) {
        throw new UnsupportedOperationException("TODO: Task 07");
    }

    @Override
    public ExchangeRate rateFor(CurrencyPair pair) {
        throw new UnsupportedOperationException("TODO: Task 07");
    }
}
