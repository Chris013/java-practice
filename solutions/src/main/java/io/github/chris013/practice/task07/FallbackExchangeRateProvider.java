package io.github.chris013.practice.task07;

import java.util.Objects;

/** Asks a secondary source when the primary one is temporarily unavailable. */
public final class FallbackExchangeRateProvider implements ExchangeRateProvider {

    private final ExchangeRateProvider primary;
    private final ExchangeRateProvider secondary;

    public FallbackExchangeRateProvider(ExchangeRateProvider primary, ExchangeRateProvider secondary) {
        this.primary = Objects.requireNonNull(primary, "primary");
        this.secondary = Objects.requireNonNull(secondary, "secondary");
    }

    @Override
    public ExchangeRate rateFor(CurrencyPair pair) {
        try {
            return primary.rateFor(pair);
        } catch (RateUnavailableException primaryFailure) {
            try {
                return secondary.rateFor(pair);
            } catch (RateUnavailableException secondaryFailure) {
                secondaryFailure.addSuppressed(primaryFailure);
                throw secondaryFailure;
            }
        }
    }
}
