package io.github.chris013.practice.task11;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.RepeatedTest;

/**
 * Time is frozen, so the only thing that can change the result is the race between threads. Each attempt is
 * started by a shared gate, so all of them hit the bucket at the same moment.
 */
class TokenBucketConcurrencyTest {

    private static final int CAPACITY = 1_000;
    private static final int ATTEMPTS = 50_000;

    private final MutableInstantSource time = new MutableInstantSource(Instant.parse("2026-01-01T12:00:00Z"));

    @RepeatedTest(5)
    void exactly_capacity_requests_succeed_when_virtual_threads_race_for_the_tokens() {
        TokenBucket bucket = new TokenBucket(CAPACITY, 1, Duration.ofSeconds(1), time);
        AtomicInteger granted = new AtomicInteger();
        CountDownLatch startGate = new CountDownLatch(1);

        // close() waits until every task has finished.
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < ATTEMPTS; i++) {
                executor.execute(() -> {
                    awaitQuietly(startGate);
                    if (bucket.tryConsume()) {
                        granted.incrementAndGet();
                    }
                });
            }
            startGate.countDown();
        }

        assertThat(granted).hasValue(CAPACITY);
        assertThat(bucket.availableTokens()).isZero();
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
