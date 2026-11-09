package io.github.chris013.practice.task07;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ExchangeRateDecoratorsTest {

    private static final CurrencyPair EUR_USD = CurrencyPair.of("EUR", "USD");
    private static final CurrencyPair EUR_GBP = CurrencyPair.of("EUR", "GBP");
    private static final Duration TEN_MINUTES = Duration.ofMinutes(10);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-03-01T09:00:00Z"));

    @Nested
    class Caching {

        private final FakeExchangeRateProvider remote =
                new FakeExchangeRateProvider("ECB").knows(EUR_USD, "1.0832").knows(EUR_GBP, "0.8571");
        private final ExchangeRateProvider rates = new CachingExchangeRateProvider(remote, clock, TEN_MINUTES);

        @Test
        void asks_the_delegate_the_first_time() {
            assertThat(rates.rateFor(EUR_USD).rate()).isEqualByComparingTo("1.0832");
            assertThat(remote.calls()).isEqualTo(1);
        }

        @Test
        void serves_repeated_requests_from_the_cache() {
            rates.rateFor(EUR_USD);
            clock.advanceBy(Duration.ofMinutes(9));

            ExchangeRate rate = rates.rateFor(EUR_USD);

            assertThat(rate.rate()).isEqualByComparingTo("1.0832");
            assertThat(remote.calls()).isEqualTo(1);
        }

        @Test
        void fetches_again_once_the_entry_has_expired() {
            rates.rateFor(EUR_USD);
            clock.advanceBy(TEN_MINUTES);

            rates.rateFor(EUR_USD);

            assertThat(remote.calls()).isEqualTo(2);
        }

        @Test
        void caches_each_pair_separately() {
            rates.rateFor(EUR_USD);
            rates.rateFor(EUR_GBP);
            rates.rateFor(EUR_USD);
            rates.rateFor(EUR_GBP);

            assertThat(remote.calls()).isEqualTo(2);
        }

        @Test
        void does_not_cache_failures() {
            remote.failsNextCalls(1);

            assertThatThrownBy(() -> rates.rateFor(EUR_USD)).isInstanceOf(RateUnavailableException.class);
            assertThat(rates.rateFor(EUR_USD).rate()).isEqualByComparingTo("1.0832");
            assertThat(remote.calls()).isEqualTo(2);
        }

        @Test
        void needs_a_positive_time_to_live() {
            assertThatThrownBy(() -> new CachingExchangeRateProvider(remote, clock, Duration.ZERO))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CachingExchangeRateProvider(remote, clock, Duration.ofSeconds(-1)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new CachingExchangeRateProvider(null, clock, TEN_MINUTES))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Retrying {

        private final FakeExchangeRateProvider remote = new FakeExchangeRateProvider("ECB").knows(EUR_USD, "1.0832");

        @Test
        void does_not_retry_when_the_first_attempt_succeeds() {
            new RetryingExchangeRateProvider(remote, 3).rateFor(EUR_USD);

            assertThat(remote.calls()).isEqualTo(1);
        }

        @Test
        void retries_temporary_failures() {
            remote.failsNextCalls(2);

            ExchangeRate rate = new RetryingExchangeRateProvider(remote, 3).rateFor(EUR_USD);

            assertThat(rate.rate()).isEqualByComparingTo("1.0832");
            assertThat(remote.calls()).isEqualTo(3);
        }

        @Test
        void gives_up_after_the_last_attempt_and_keeps_every_failure() {
            remote.alwaysFails();
            ExchangeRateProvider rates = new RetryingExchangeRateProvider(remote, 3);

            assertThatThrownBy(() -> rates.rateFor(EUR_USD))
                    .isInstanceOf(RateUnavailableException.class)
                    .satisfies(failure -> assertThat(failure.getSuppressed()).hasSize(2));
            assertThat(remote.calls()).isEqualTo(3);
        }

        @Test
        void does_not_retry_errors_that_retrying_cannot_fix() {
            ExchangeRateProvider rates = new RetryingExchangeRateProvider(remote, 3);

            assertThatThrownBy(() -> rates.rateFor(EUR_GBP)).isInstanceOf(IllegalArgumentException.class);
            assertThat(remote.calls()).isEqualTo(1);
        }

        @Test
        void needs_at_least_one_attempt() {
            assertThatThrownBy(() -> new RetryingExchangeRateProvider(remote, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Fallback {

        private final FakeExchangeRateProvider primary = new FakeExchangeRateProvider("ECB").knows(EUR_USD, "1.0832");
        private final FakeExchangeRateProvider secondary = new FakeExchangeRateProvider("Backup").knows(EUR_USD, "1.0829");
        private final ExchangeRateProvider rates = new FallbackExchangeRateProvider(primary, secondary);

        @Test
        void prefers_the_primary_source() {
            assertThat(rates.rateFor(EUR_USD).rate()).isEqualByComparingTo("1.0832");
            assertThat(secondary.calls()).isZero();
        }

        @Test
        void uses_the_secondary_source_when_the_primary_is_unavailable() {
            primary.alwaysFails();

            assertThat(rates.rateFor(EUR_USD).rate()).isEqualByComparingTo("1.0829");
        }

        @Test
        void reports_both_failures_when_both_sources_are_unavailable() {
            primary.alwaysFails();
            secondary.alwaysFails();

            assertThatThrownBy(() -> rates.rateFor(EUR_USD))
                    .isInstanceOf(RateUnavailableException.class)
                    .hasMessageContaining("Backup")
                    .satisfies(failure -> assertThat(failure.getSuppressed())
                            .singleElement()
                            .satisfies(suppressed -> assertThat(suppressed).hasMessageContaining("ECB")));
        }
    }

    @Test
    void decorators_compose_into_a_resilient_cached_provider() {
        FakeExchangeRateProvider ecb = new FakeExchangeRateProvider("ECB").knows(EUR_USD, "1.0832").failsNextCalls(1);
        FakeExchangeRateProvider backup = new FakeExchangeRateProvider("Backup").alwaysFails();
        ExchangeRateProvider rates = new CachingExchangeRateProvider(
                new RetryingExchangeRateProvider(new FallbackExchangeRateProvider(ecb, backup), 2),
                clock,
                TEN_MINUTES);

        BigDecimal first = rates.rateFor(EUR_USD).rate();
        BigDecimal second = rates.rateFor(EUR_USD).rate();

        assertThat(first).isEqualByComparingTo("1.0832");
        assertThat(second).isEqualByComparingTo("1.0832");
        assertThat(ecb.calls()).as("ECB: failed once, then succeeded on retry, then cached").isEqualTo(2);
        assertThat(backup.calls()).as("backup was tried once during the failed first attempt").isEqualTo(1);
    }
}
