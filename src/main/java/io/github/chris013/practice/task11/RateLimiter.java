package io.github.chris013.practice.task11;

import java.time.Duration;
import java.time.InstantSource;

/**
 * One {@link TokenBucket} per key, for example one per API key. A bucket is created the first time its key is seen.
 *
 * <p>Task 11: read {@code tasks/11-token-bucket-rate-limiter.md} first.
 */
public final class RateLimiter<K> {

    /**
     * Every key gets a bucket with these settings.
     *
     * @throws IllegalArgumentException if {@code capacity} or {@code refillTokens} is below 1, or {@code refillPeriod}
     *     is not positive
     * @throws NullPointerException if {@code refillPeriod} or {@code time} is null
     */
    public RateLimiter(long capacity, long refillTokens, Duration refillPeriod, InstantSource time) {
        throw new UnsupportedOperationException("TODO: Task 11");
    }

    /**
     * Takes one token from the bucket of {@code key}, creating that bucket if this is the key's first request.
     * Exactly one bucket is created per key, even when many threads ask for the same new key at once.
     */
    public boolean tryAcquire(K key) {
        throw new UnsupportedOperationException("TODO: Task 11");
    }

    /** Tokens left for {@code key}. A key we have never seen has a full bucket. */
    public long availableTokens(K key) {
        throw new UnsupportedOperationException("TODO: Task 11");
    }
}
