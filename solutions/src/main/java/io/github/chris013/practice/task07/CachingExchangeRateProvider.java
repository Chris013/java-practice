package io.github.chris013.practice.task07;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers each rate for {@code timeToLive}, so we don't hit the remote API for every price we display.
 * Failures are never cached: the next request tries again.
 */
public final class CachingExchangeRateProvider implements ExchangeRateProvider {

    private final ExchangeRateProvider delegate;
    private final Clock clock;
    private final Duration timeToLive;
    private final Map<CurrencyPair, CachedRate> cache = new ConcurrentHashMap<>();

    public CachingExchangeRateProvider(ExchangeRateProvider delegate, Clock clock, Duration timeToLive) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.timeToLive = Objects.requireNonNull(timeToLive, "timeToLive");
        if (timeToLive.isZero() || timeToLive.isNegative()) {
            throw new IllegalArgumentException("timeToLive must be positive, was " + timeToLive);
        }
    }

    @Override
    public ExchangeRate rateFor(CurrencyPair pair) {
        Instant now = clock.instant();
        CachedRate cached = cache.get(pair);
        if (cached != null && cached.isFreshAt(now)) {
            return cached.rate();
        }
        ExchangeRate fresh = delegate.rateFor(pair);
        cache.put(pair, new CachedRate(fresh, now.plus(timeToLive)));
        return fresh;
    }

    private record CachedRate(ExchangeRate rate, Instant expiresAt) {

        boolean isFreshAt(Instant now) {
            return now.isBefore(expiresAt);
        }
    }
}
