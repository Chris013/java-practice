package io.github.chris013.practice.task07;

/** A source of exchange rates. The real implementations call slow, flaky, rate-limited HTTP APIs. */
public interface ExchangeRateProvider {

    /**
     * @throws RateUnavailableException if the rate can't be fetched right now
     * @throws IllegalArgumentException if this provider doesn't support the pair at all (retrying won't help)
     */
    ExchangeRate rateFor(CurrencyPair pair);
}
