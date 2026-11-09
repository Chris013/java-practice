package io.github.chris013.practice.task11;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A token bucket: holds up to {@code capacity} tokens and gets {@code refillTokens} back every full
 * {@code refillPeriod}. A request takes its tokens from the bucket or is refused.
 *
 * <p>The refill is done in whole periods and in integers only. The refill clock only moves forward by whole
 * periods, so the part of a period that has not finished yet is kept for the next call and no time is lost.
 *
 * <p>Thread safety: all state lives in one immutable {@link State} object held by an {@link AtomicReference}.
 * {@link #tryConsume(long)} is a compare-and-set loop, so it never blocks and never loses an update.
 */
public final class TokenBucket {

    private final long capacity;
    private final long refillTokens;
    private final Duration refillPeriod;
    private final InstantSource time;
    private final AtomicReference<State> state;

    /**
     * Creates a bucket that starts full.
     *
     * @throws IllegalArgumentException if {@code capacity} or {@code refillTokens} is below 1, or {@code refillPeriod}
     *     is not positive
     * @throws NullPointerException if {@code refillPeriod} or {@code time} is null
     */
    public TokenBucket(long capacity, long refillTokens, Duration refillPeriod, InstantSource time) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1, was " + capacity);
        }
        if (refillTokens < 1) {
            throw new IllegalArgumentException("refillTokens must be at least 1, was " + refillTokens);
        }
        Objects.requireNonNull(refillPeriod, "refillPeriod");
        if (refillPeriod.isZero() || refillPeriod.isNegative()) {
            throw new IllegalArgumentException("refillPeriod must be positive, was " + refillPeriod);
        }
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillPeriod = refillPeriod;
        this.time = Objects.requireNonNull(time, "time");
        this.state = new AtomicReference<>(new State(capacity, time.instant()));
    }

    /** Takes one token if there is one. Same as {@code tryConsume(1)}. */
    public boolean tryConsume() {
        return tryConsume(1);
    }

    /**
     * Takes {@code tokens} tokens if there are that many after the refill. Otherwise takes nothing and returns false.
     *
     * @throws IllegalArgumentException if {@code tokens} is below 1 or above the capacity
     */
    public boolean tryConsume(long tokens) {
        if (tokens < 1 || tokens > capacity) {
            throw new IllegalArgumentException("tokens must be between 1 and " + capacity + ", was " + tokens);
        }
        // Read the clock once per attempt. On a retry we reuse this instant: the state we see may have been
        // refilled by another thread with a later reading, and refreshed() treats that as "no time passed".
        Instant now = time.instant();
        while (true) {
            State current = state.get();
            State refreshed = refreshed(current, now);
            if (refreshed.tokens() < tokens) {
                return false; // refused: nothing is written, so nothing is taken
            }
            State next = new State(refreshed.tokens() - tokens, refreshed.refilledAt());
            // compareAndSet succeeds only if no other thread changed the state since we read it. If one did, we
            // start again from the new state.
            if (state.compareAndSet(current, next)) {
                return true;
            }
        }
    }

    /** The number of tokens available right now, after applying the refill up to the current instant. */
    public long availableTokens() {
        return refreshed(state.get(), time.instant()).tokens();
    }

    /**
     * Returns the state as it would be at {@code now}, with the refill applied. Never changes {@code current}.
     */
    private State refreshed(State current, Instant now) {
        if (now.isBefore(current.refilledAt())) {
            // The clock went backwards. Treat it as zero time elapsed so tokens are neither added nor removed.
            return current;
        }
        if (current.tokens() >= capacity) {
            // A full bucket has nothing to refill. Its refill clock starts now, so that after spending from it the
            // next refill comes one full period later.
            return new State(capacity, now);
        }
        long periods = Duration.between(current.refilledAt(), now).dividedBy(refillPeriod);
        if (periods == 0) {
            return current;
        }
        long missing = capacity - current.tokens();
        if (periods >= Math.ceilDiv(missing, refillTokens)) {
            // Enough periods passed to fill the bucket. It is full now, so the refill clock starts now.
            return new State(capacity, now);
        }
        // Fewer periods than needed to fill the bucket: no overflow is possible, and the refill clock moves forward
        // by whole periods only. The part of the current period that is still running is kept.
        return new State(
                current.tokens() + periods * refillTokens,
                current.refilledAt().plus(refillPeriod.multipliedBy(periods)));
    }

    /**
     * An immutable snapshot of the bucket.
     *
     * @param tokens     tokens in the bucket as of {@code refilledAt}
     * @param refilledAt the point up to which whole refill periods have been counted
     */
    private record State(long tokens, Instant refilledAt) {
    }
}
