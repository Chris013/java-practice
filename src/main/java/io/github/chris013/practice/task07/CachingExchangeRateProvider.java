package io.github.chris013.practice.task07;

import java.time.Clock;
import java.time.Duration;

/**
 * Remembers each rate for {@code timeToLive}, so we don't hit the remote API for every price we display.
 *
 * <p>Task 07: read {@code tasks/07-exchange-rate-decorators.md} first.
 */
public final class CachingExchangeRateProvider implements ExchangeRateProvider {

    public CachingExchangeRateProvider(ExchangeRateProvider delegate, Clock clock, Duration timeToLive) {
        throw new UnsupportedOperationException("TODO: Task 07");
    }

    @Override
    public ExchangeRate rateFor(CurrencyPair pair) {
        throw new UnsupportedOperationException("TODO: Task 07");
    }
}
