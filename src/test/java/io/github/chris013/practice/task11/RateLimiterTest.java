package io.github.chris013.practice.task11;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private static final Instant START = Instant.parse("2026-01-01T12:00:00Z");
    private static final Duration PERIOD = Duration.ofSeconds(1);

    private final MutableInstantSource time = new MutableInstantSource(START);

    @Test
    void each_key_gets_its_own_bucket() {
        RateLimiter<String> limiter = new RateLimiter<>(2, 1, PERIOD, time);

        assertThat(limiter.tryAcquire("alice")).isTrue();
        assertThat(limiter.tryAcquire("alice")).isTrue();
        assertThat(limiter.tryAcquire("alice")).isFalse();

        assertThat(limiter.tryAcquire("bob")).isTrue();
        assertThat(limiter.availableTokens("alice")).isZero();
        assertThat(limiter.availableTokens("bob")).isEqualTo(1);
    }

    @Test
    void a_key_we_have_never_seen_has_full_capacity() {
        RateLimiter<String> limiter = new RateLimiter<>(7, 1, PERIOD, time);

        assertThat(limiter.availableTokens("nobody")).isEqualTo(7);
    }

    @Test
    void each_key_refills_on_its_own_schedule() {
        RateLimiter<String> limiter = new RateLimiter<>(1, 1, PERIOD, time);
        limiter.tryAcquire("alice"); // alice's clock starts at 0 s

        time.advance(Duration.ofMillis(500));
        limiter.tryAcquire("bob"); // bob's clock starts at 0.5 s

        time.advance(Duration.ofMillis(500)); // now 1 s
        assertThat(limiter.availableTokens("alice")).isEqualTo(1);
        assertThat(limiter.availableTokens("bob")).isZero();
    }

    @Test
    void rejects_null_keys() {
        RateLimiter<String> limiter = new RateLimiter<>(1, 1, PERIOD, time);

        assertThatThrownBy(() -> limiter.tryAcquire(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("key");
    }

    @Test
    void rejects_invalid_limits_when_it_is_built() {
        assertThatThrownBy(() -> new RateLimiter<String>(0, 1, PERIOD, time))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RateLimiter<String>(1, 1, Duration.ZERO, time))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Nested
    class Concurrency {

        @Test
        void ten_thousand_first_requests_for_one_new_key_create_exactly_one_bucket() {
            int capacity = 100;
            RateLimiter<String> limiter = new RateLimiter<>(capacity, 10, PERIOD, time);
            AtomicInteger granted = new AtomicInteger();
            CountDownLatch startGate = new CountDownLatch(1);

            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int i = 0; i < 10_000; i++) {
                    executor.execute(() -> {
                        awaitQuietly(startGate);
                        if (limiter.tryAcquire("parcel-api-key")) {
                            granted.incrementAndGet();
                        }
                    });
                }
                startGate.countDown();
            }

            // If two buckets had been created for the key, each would hand out its own 100 tokens.
            assertThat(granted).hasValue(capacity);
            assertThat(limiter.availableTokens("parcel-api-key")).isZero();
        }

        private static void awaitQuietly(CountDownLatch latch) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted while waiting for the start gate", e);
            }
        }
    }
}
