package io.github.chris013.practice.task11;

import java.time.Duration;
import java.time.InstantSource;

/**
 * A token bucket: holds up to {@code capacity} tokens and gets {@code refillTokens} back every full
 * {@code refillPeriod}. A request takes its tokens from the bucket or is refused.
 *
 * <p>Starts full. Safe to use from many threads at once.
 *
 * <p>Task 11: read {@code tasks/11-token-bucket-rate-limiter.md} first.
 */
public final class TokenBucket {

    /**
     * Creates a bucket that starts full.
     *
     * @throws IllegalArgumentException if {@code capacity} or {@code refillTokens} is below 1, or {@code refillPeriod}
     *     is not positive
     * @throws NullPointerException if {@code refillPeriod} or {@code time} is null
     */
    public TokenBucket(long capacity, long refillTokens, Duration refillPeriod, InstantSource time) {
        throw new UnsupportedOperationException("TODO: Task 11");
    }

    /** Takes one token if there is one. Same as {@code tryConsume(1)}. */
    public boolean tryConsume() {
        throw new UnsupportedOperationException("TODO: Task 11");
    }

    /**
     * Takes {@code tokens} tokens if there are that many after the refill. Otherwise takes nothing and returns false.
     *
     * @throws IllegalArgumentException if {@code tokens} is below 1 or above the capacity
     */
    public boolean tryConsume(long tokens) {
        throw new UnsupportedOperationException("TODO: Task 11");
    }

    /** The number of tokens available right now, after applying the refill up to the current instant. */
    public long availableTokens() {
        throw new UnsupportedOperationException("TODO: Task 11");
    }
}
