package io.github.chris013.practice.task23;

import static io.github.chris013.practice.task23.FakeProvider.answering;
import static io.github.chris013.practice.task23.FakeProvider.answeringAfter;
import static io.github.chris013.practice.task23.FakeProvider.failing;
import static io.github.chris013.practice.task23.FakeProvider.failingWith;
import static io.github.chris013.practice.task23.FakeProvider.quote;
import static io.github.chris013.practice.task23.FakeProvider.slowAnswering;
import static io.github.chris013.practice.task23.FakeProvider.throwingError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.chris013.practice.task23.FakeProvider.Hanging;
import io.github.chris013.practice.task23.ProviderFailure.Reason;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class QuoteAggregatorTest {

    private static final TripRequest REQUEST = new TripRequest("AMS", "LIS", LocalDate.of(2027, 3, 1), 2);
    private static final Duration BUDGET = Duration.ofMillis(200);

    @Nested
    class AllProvidersAnswer {

        @Test
        void returns_all_quotes_sorted_by_price_and_is_complete() throws Exception {
            try (var aggregator = new QuoteAggregator(List.of(
                    answering("KLM", quote("KLM", "149.00", "KL1601")),
                    answering("TAP", quote("TAP", "99.90", "TP1234"), quote("TAP", "120.00", "TP1236"))), BUDGET)) {

                AggregatedQuotes result = aggregator.quotesFor(REQUEST);

                assertThat(result.quotes()).extracting(Quote::price).containsExactly(
                        new BigDecimal("99.90"), new BigDecimal("120.00"),
                        new BigDecimal("149.00"));
                assertThat(result.failures()).isEmpty();
                assertThat(result.isComplete()).isTrue();
                assertThat(result.cheapest()).map(Quote::provider).contains("TAP");
            }
        }

        @Test
        void breaks_price_ties_by_provider_name_then_flight_number() throws Exception {
            try (var aggregator = new QuoteAggregator(List.of(
                    answering("Iberia", quote("Iberia", "100.00", "IB2")),
                    answering("Air France", quote("Air France", "100.00", "AF9")),
                    answering("Iberia Express", quote("Iberia", "100.00", "IB1"))), BUDGET)) {

                AggregatedQuotes result = aggregator.quotesFor(REQUEST);

                assertThat(result.quotes()).extracting(Quote::provider, Quote::flightNumber).containsExactly(
                        Tuple.tuple("Air France", "AF9"),
                        Tuple.tuple("Iberia", "IB1"),
                        Tuple.tuple("Iberia", "IB2"));
            }
        }

        @Test
        void has_no_cheapest_quote_when_nobody_quoted() throws Exception {
            try (var aggregator = new QuoteAggregator(List.of(answering("Empty")), BUDGET)) {
                assertThat(aggregator.quotesFor(REQUEST).cheapest()).isEmpty();
            }
        }
    }

    @Nested
    class OneProviderFails {

        @Test
        void keeps_the_other_quotes_and_reports_the_error_with_its_message() throws Exception {
            try (var aggregator = new QuoteAggregator(List.of(
                    failing("Broken"),
                    answering("TAP", quote("TAP", "99.90", "TP1234"))), BUDGET)) {

                AggregatedQuotes result = aggregator.quotesFor(REQUEST);

                assertThat(result.quotes()).extracting(Quote::provider).containsExactly("TAP");
                assertThat(result.failures()).containsExactly(
                        new ProviderFailure("Broken", Reason.ERROR, "IOException: 503"));
                assertThat(result.isComplete()).isFalse();
            }
        }

        @Test
        void gives_no_quotes_and_no_cheapest_when_every_provider_fails() throws Exception {
            try (var aggregator = new QuoteAggregator(List.of(failing("A"), failing("B")), BUDGET)) {

                AggregatedQuotes result = aggregator.quotesFor(REQUEST);

                assertThat(result.quotes()).isEmpty();
                assertThat(result.cheapest()).isEmpty();
                assertThat(result.failures()).extracting(ProviderFailure::provider).containsExactly("A", "B");
            }
        }

        @Test
        void reports_only_the_class_name_when_the_exception_has_no_message() throws Exception {
            try (var aggregator = new QuoteAggregator(List.of(
                    failingWith("Silent", new IllegalStateException())), BUDGET)) {

                AggregatedQuotes result = aggregator.quotesFor(REQUEST);

                assertThat(result.failures()).containsExactly(
                        new ProviderFailure("Silent", Reason.ERROR, "IllegalStateException"));
            }
        }

        @Test
        void an_error_in_a_provider_is_not_turned_into_a_failure() throws Exception {
            try (var aggregator = new QuoteAggregator(List.of(throwingError("Broken")), BUDGET)) {
                assertThatThrownBy(() -> aggregator.quotesFor(REQUEST))
                        .isInstanceOf(StackOverflowError.class);
            }
        }
    }

    @Nested
    class TimeBudget {

        @Test
        void a_hanging_provider_is_reported_as_timeout_after_about_the_budget() throws Exception {
            Hanging hanging = FakeProvider.hanging("Stuck");
            try (var aggregator = new QuoteAggregator(List.of(hanging), BUDGET)) {
                long start = System.nanoTime();

                AggregatedQuotes result = aggregator.quotesFor(REQUEST);

                Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
                assertThat(elapsed).isLessThan(Duration.ofMillis(1_500));
                assertThat(elapsed).isGreaterThanOrEqualTo(Duration.ofMillis(150));
                assertThat(result.quotes()).isEmpty();
                assertThat(result.failures()).extracting(ProviderFailure::reason).containsExactly(Reason.TIMEOUT);
            }
        }

        @Test
        void a_hanging_provider_is_interrupted_when_the_budget_expires() throws Exception {
            Hanging hanging = FakeProvider.hanging("Stuck");
            try (var aggregator = new QuoteAggregator(List.of(hanging), BUDGET)) {
                aggregator.quotesFor(REQUEST);

                assertThat(hanging.awaitInterrupted(Duration.ofSeconds(1))).isTrue();
            }
        }

        @Test
        void keeps_a_provider_that_answers_within_the_budget() throws Exception {
            try (var aggregator = new QuoteAggregator(List.of(
                    slowAnswering("Slow", Duration.ofMillis(50), quote("Slow", "80.00", "SL1")),
                    FakeProvider.hanging("Stuck")), BUDGET)) {

                AggregatedQuotes result = aggregator.quotesFor(REQUEST);

                assertThat(result.quotes()).extracting(Quote::provider).containsExactly("Slow");
                assertThat(result.failures()).extracting(ProviderFailure::provider).containsExactly("Stuck");
            }
        }

        @Test
        void the_budget_is_shared_by_all_providers_not_added_up_per_provider() throws Exception {
            // Four hanging providers. Waiting for each one in turn with its own timeout would take 4 x 200 ms.
            try (var aggregator = new QuoteAggregator(List.of(
                    FakeProvider.hanging("A"), FakeProvider.hanging("B"),
                    FakeProvider.hanging("C"), FakeProvider.hanging("D")), BUDGET)) {
                long start = System.nanoTime();

                AggregatedQuotes result = aggregator.quotesFor(REQUEST);

                Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
                assertThat(elapsed).isLessThan(Duration.ofMillis(700));
                assertThat(result.failures()).hasSize(4);
            }
        }
    }

    @Nested
    class Parallelism {

        @Test
        void providers_run_at_the_same_time() throws Exception {
            // If the providers ran one after another, the first would wait at the barrier until the budget
            // expired, and the result would not be complete.
            CyclicBarrier barrier = new CyclicBarrier(5);
            List<QuoteProvider> providers = List.of(
                    answeringAfter("P1", barrier, quote("P1", "10.00", "F1")),
                    answeringAfter("P2", barrier, quote("P2", "10.00", "F2")),
                    answeringAfter("P3", barrier, quote("P3", "10.00", "F3")),
                    answeringAfter("P4", barrier, quote("P4", "10.00", "F4")),
                    answeringAfter("P5", barrier, quote("P5", "10.00", "F5")));
            try (var aggregator = new QuoteAggregator(providers, Duration.ofSeconds(1))) {

                AggregatedQuotes result = aggregator.quotesFor(REQUEST);

                assertThat(result.quotes()).hasSize(5);
                assertThat(result.isComplete()).isTrue();
            }
        }
    }

    @Nested
    class CallerInterrupted {

        @Test
        void throws_interrupted_exception_and_cancels_the_providers() throws Exception {
            Hanging hanging = FakeProvider.hanging("Stuck");
            AtomicReference<InterruptedException> thrown = new AtomicReference<>();
            try (var aggregator = new QuoteAggregator(List.of(hanging), Duration.ofSeconds(10))) {
                Thread caller = Thread.ofVirtual().start(() -> {
                    try {
                        aggregator.quotesFor(REQUEST);
                    } catch (InterruptedException e) {
                        thrown.set(e);
                    }
                });

                assertThat(hanging.awaitStarted(Duration.ofSeconds(1))).isTrue();
                caller.interrupt();

                assertThat(caller.join(Duration.ofSeconds(2))).isTrue();
                assertThat(thrown.get()).isInstanceOf(InterruptedException.class);
                assertThat(hanging.awaitInterrupted(Duration.ofSeconds(1))).isTrue();
            }
        }
    }

    @Nested
    class Closing {

        @Test
        void close_interrupts_a_search_that_is_still_running() throws Exception {
            Hanging hanging = FakeProvider.hanging("Stuck");
            var aggregator = new QuoteAggregator(List.of(hanging), Duration.ofSeconds(10));
            try {
                AtomicReference<AggregatedQuotes> result = new AtomicReference<>();
                Thread caller = Thread.ofVirtual().start(() -> {
                    try {
                        result.set(aggregator.quotesFor(REQUEST));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                assertThat(hanging.awaitStarted(Duration.ofSeconds(1))).isTrue();

                Thread closer = Thread.ofVirtual().start(aggregator::close);

                assertThat(hanging.awaitInterrupted(Duration.ofSeconds(1))).isTrue();
                assertThat(closer.join(Duration.ofSeconds(1))).isTrue();
                assertThat(caller.join(Duration.ofSeconds(1))).isTrue();
                assertThat(result.get().failures())
                        .containsExactly(new ProviderFailure("Stuck", Reason.ERROR, "InterruptedException"));
            } finally {
                aggregator.close(); // closing twice is harmless
            }
        }
    }

    @Nested
    class Construction {

        @Test
        void rejects_a_null_budget_naming_the_parameter() {
            assertThatThrownBy(() -> new QuoteAggregator(List.of(answering("A")), null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("budget");
        }

        @Test
        void rejects_an_empty_provider_list() {
            assertThatThrownBy(() -> new QuoteAggregator(List.of(), BUDGET))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_null_providers_naming_the_parameter() {
            assertThatThrownBy(() -> new QuoteAggregator(null, BUDGET))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("providers");
        }

        @ParameterizedTest
        @CsvSource({"0", "-1"})
        void rejects_a_budget_that_is_not_positive(long millis) {
            assertThatThrownBy(() -> new QuoteAggregator(List.of(answering("A")), Duration.ofMillis(millis)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_a_null_request() throws Exception {
            try (var aggregator = new QuoteAggregator(List.of(answering("A")), BUDGET)) {
                assertThatThrownBy(() -> aggregator.quotesFor(null))
                        .isInstanceOf(NullPointerException.class)
                        .hasMessageContaining("request");
            }
        }
    }
}
