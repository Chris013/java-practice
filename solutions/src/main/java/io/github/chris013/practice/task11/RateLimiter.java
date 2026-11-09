package io.github.chris013.practice.task11;

import java.time.Duration;
import java.time.InstantSource;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * One {@link TokenBucket} per key, for example one per API key. A bucket is created the first time its key is seen.
 *
 * <p>Buckets are never removed. That is fine for a few thousand API keys. For more, see the stretch goals.
 */
public final class RateLimiter<K> {

    private final long capacity;
    private final Supplier<TokenBucket> newBucket;
    private final Map<K, TokenBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Every key gets a bucket with these settings.
     *
     * @throws IllegalArgumentException if {@code capacity} or {@code refillTokens} is below 1, or {@code refillPeriod}
     *     is not positive
     * @throws NullPointerException if {@code refillPeriod} or {@code time} is null
     */
    public RateLimiter(long capacity, long refillTokens, Duration refillPeriod, InstantSource time) {
        this.capacity = capacity;
        this.newBucket = () -> new TokenBucket(capacity, refillTokens, refillPeriod, time);
        // Build one bucket now, so bad arguments fail at construction and not on the first request.
        newBucket.get();
    }

    /**
     * Takes one token from the bucket of {@code key}, creating that bucket if this is the key's first request.
     */
    public boolean tryAcquire(K key) {
        Objects.requireNonNull(key, "key");
        // computeIfAbsent runs the factory at most once per key, even under contention. A get-then-put would let two
        // threads each create a bucket, and one of them would then be thrown away together with its tokens.
        return buckets.computeIfAbsent(key, k -> newBucket.get()).tryConsume();
    }

    /** Tokens left for {@code key}. A key we have never seen has a full bucket, and asking does not create one. */
    public long availableTokens(K key) {
        TokenBucket bucket = buckets.get(Objects.requireNonNull(key, "key"));
        return bucket == null ? capacity : bucket.availableTokens();
    }
}
