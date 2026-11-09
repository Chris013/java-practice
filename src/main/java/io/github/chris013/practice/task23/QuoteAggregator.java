package io.github.chris013.practice.task23;

import java.time.Duration;
import java.util.List;

/**
 * Asks all providers at the same time and returns after at most {@code budget}.
 *
 * <p>Task 23: read {@code tasks/23-quote-aggregator.md} first.
 */
public final class QuoteAggregator implements AutoCloseable {

    /**
     * @throws IllegalArgumentException if {@code providers} is empty or {@code budget} is not positive
     */
    public QuoteAggregator(List<QuoteProvider> providers, Duration budget) {
        throw new UnsupportedOperationException("TODO: Task 23");
    }

    /**
     * Calls every provider concurrently and waits at most the budget in total. Providers that have not
     * answered by then are cancelled with interruption and reported as {@link ProviderFailure.Reason#TIMEOUT}.
     *
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    public AggregatedQuotes quotesFor(TripRequest request) throws InterruptedException {
        throw new UnsupportedOperationException("TODO: Task 23");
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("TODO: Task 23");
    }
}
