package io.github.chris013.practice.task23;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The result of one search: every quote that arrived in time, and every provider that did not deliver.
 * Given complete, so that you can focus on the concurrency.
 */
public record AggregatedQuotes(List<Quote> quotes, List<ProviderFailure> failures) {

    private static final Comparator<Quote> CHEAPEST_FIRST = Comparator.comparing(Quote::price)
            .thenComparing(Quote::provider)
            .thenComparing(Quote::flightNumber);
    private static final Comparator<ProviderFailure> BY_PROVIDER = Comparator.comparing(ProviderFailure::provider);

    public AggregatedQuotes {
        Objects.requireNonNull(quotes, "quotes");
        Objects.requireNonNull(failures, "failures");
        quotes = quotes.stream().sorted(CHEAPEST_FIRST).toList();
        failures = failures.stream().sorted(BY_PROVIDER).toList();
    }

    public Optional<Quote> cheapest() {
        return quotes.isEmpty() ? Optional.empty() : Optional.of(quotes.getFirst());
    }

    /** True when every provider answered in time without an error. */
    public boolean isComplete() {
        return failures.isEmpty();
    }
}
