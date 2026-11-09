package io.github.chris013.practice.task23;

import io.github.chris013.practice.task23.ProviderFailure.Reason;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Asks all providers at the same time and returns after at most {@code budget}.
 *
 * <p>Each provider runs on its own virtual thread. Virtual threads make a blocking HTTP call cheap, so we
 * can simply block and let the scheduler park the thread. The budget is one deadline for the whole search,
 * not one per provider: all providers start at the same moment, so the slowest one decides the wait.
 *
 * <p>Task 23: read {@code tasks/23-quote-aggregator.md} first.
 */
public final class QuoteAggregator implements AutoCloseable {

    private final List<QuoteProvider> providers;
    private final Duration budget;
    private final ExecutorService executor;

    /**
     * @throws IllegalArgumentException if {@code providers} is empty or {@code budget} is not positive
     */
    public QuoteAggregator(List<QuoteProvider> providers, Duration budget) {
        Objects.requireNonNull(providers, "providers");
        Objects.requireNonNull(budget, "budget");
        if (providers.isEmpty()) {
            throw new IllegalArgumentException("At least one quote provider is needed");
        }
        if (budget.isNegative() || budget.isZero()) {
            throw new IllegalArgumentException("The budget must be positive, got " + budget);
        }
        this.providers = List.copyOf(providers);
        this.budget = budget;
        // Created last, so a failed validation above does not leak an executor.
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Calls every provider concurrently and waits at most the budget in total. Providers that have not
     * answered by then are cancelled with interruption and reported as {@link Reason#TIMEOUT}.
     *
     * <p>We declare {@code InterruptedException} instead of swallowing it. If the caller's thread is
     * interrupted, {@code invokeAll} cancels the provider tasks too, and the caller decides what to do.
     *
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    public AggregatedQuotes quotesFor(TripRequest request) throws InterruptedException {
        Objects.requireNonNull(request, "request");
        List<Callable<List<Quote>>> tasks = providers.stream()
                .<Callable<List<Quote>>>map(provider -> () -> provider.quotesFor(request))
                .toList();

        // invokeAll returns once every task is done, or when the budget runs out. Unfinished tasks are
        // cancelled with interruption. The futures come back in the same order as the tasks.
        List<Future<List<Quote>>> futures = executor.invokeAll(tasks, budget.toNanos(), TimeUnit.NANOSECONDS);

        List<Quote> quotes = new ArrayList<>();
        List<ProviderFailure> failures = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            String provider = providers.get(i).name();
            Future<List<Quote>> future = futures.get(i);
            switch (future.state()) {
                case SUCCESS -> quotes.addAll(future.resultNow());
                case FAILED -> {
                    Throwable cause = future.exceptionNow();
                    if (cause instanceof Error error) {
                        throw error; // a broken JVM or library, not an answer from the provider
                    }
                    failures.add(new ProviderFailure(provider, Reason.ERROR, describe(cause)));
                }
                case CANCELLED -> failures.add(new ProviderFailure(
                        provider, Reason.TIMEOUT, "no answer within " + budget.toMillis() + " ms"));
                case RUNNING -> throw new IllegalStateException("invokeAll returned before " + provider + " finished");
            }
        }
        return new AggregatedQuotes(quotes, failures);
    }

    /**
     * Shuts the executor down. {@code shutdownNow()} interrupts the providers that are still running, and
     * {@code close()} then waits until they have reacted. A provider that ignores interruption would still
     * block {@code close()}.
     */
    @Override
    public void close() {
        executor.shutdownNow();
        executor.close();
    }

    private static String describe(Throwable failure) {
        String type = failure.getClass().getSimpleName();
        return failure.getMessage() == null ? type : type + ": " + failure.getMessage();
    }
}
