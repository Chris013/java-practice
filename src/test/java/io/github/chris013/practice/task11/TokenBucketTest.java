package io.github.chris013.practice.task11;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TokenBucketTest {

    private static final Instant START = Instant.parse("2026-01-01T12:00:00Z");
    private static final Duration PERIOD = Duration.ofSeconds(1);

    private final MutableInstantSource time = new MutableInstantSource(START);

    private TokenBucket bucket(long capacity, long refillTokens) {
        return new TokenBucket(capacity, refillTokens, PERIOD, time);
    }

    @Nested
    class Consuming {

        @Test
        void starts_full() {
            assertThat(bucket(5, 1).availableTokens()).isEqualTo(5);
        }

        @Test
        void gives_out_one_token_per_call_until_empty() {
            TokenBucket bucket = bucket(3, 1);

            assertThat(bucket.tryConsume()).isTrue();
            assertThat(bucket.tryConsume()).isTrue();
            assertThat(bucket.tryConsume()).isTrue();
            assertThat(bucket.tryConsume()).isFalse();
            assertThat(bucket.availableTokens()).isZero();
        }

        @Test
        void consumes_several_tokens_at_once() {
            TokenBucket bucket = bucket(10, 1);

            assertThat(bucket.tryConsume(4)).isTrue();
            assertThat(bucket.availableTokens()).isEqualTo(6);
            assertThat(bucket.tryConsume(6)).isTrue();
            assertThat(bucket.availableTokens()).isZero();
        }

        @Test
        void refuses_without_taking_any_token_when_there_are_not_enough() {
            TokenBucket bucket = bucket(5, 1);
            assertThat(bucket.tryConsume(3)).isTrue();

            assertThat(bucket.tryConsume(3)).isFalse();
            assertThat(bucket.availableTokens()).isEqualTo(2);
            assertThat(bucket.tryConsume(2)).isTrue();
        }

        @ParameterizedTest
        @CsvSource({"0", "-1", "6"})
        void rejects_a_token_count_outside_1_to_capacity_and_takes_nothing(long tokens) {
            TokenBucket bucket = bucket(5, 1);

            assertThatThrownBy(() -> bucket.tryConsume(tokens)).isInstanceOf(IllegalArgumentException.class);
            assertThat(bucket.availableTokens()).isEqualTo(5);
        }
    }

    @Nested
    class Refilling {

        @Test
        void adds_refill_tokens_once_each_full_period() {
            TokenBucket bucket = bucket(10, 2);
            assertThat(bucket.tryConsume(10)).isTrue();

            time.advance(Duration.ofMillis(999));
            assertThat(bucket.availableTokens()).isZero();

            time.advance(Duration.ofMillis(1));
            assertThat(bucket.availableTokens()).isEqualTo(2);
        }

        @Test
        void adds_all_the_periods_that_elapsed_at_once() {
            TokenBucket bucket = bucket(10, 2);
            bucket.tryConsume(10);

            time.advance(PERIOD.multipliedBy(3));

            assertThat(bucket.availableTokens()).isEqualTo(6);
        }

        @Test
        void never_goes_above_capacity() {
            TokenBucket bucket = bucket(5, 2);
            bucket.tryConsume(5);

            time.advance(PERIOD.multipliedBy(100));

            assertThat(bucket.availableTokens()).isEqualTo(5);
        }

        @Test
        void caps_the_refill_when_one_period_would_overshoot_the_capacity() {
            TokenBucket bucket = bucket(5, 2);
            bucket.tryConsume(1); // 4 left

            time.advance(PERIOD);

            assertThat(bucket.availableTokens()).isEqualTo(5); // 4 + 2 would be 6
        }

        @Test
        void keeps_the_part_of_a_period_that_has_not_finished_yet() {
            TokenBucket bucket = bucket(10, 1);
            bucket.tryConsume(10);

            time.advance(Duration.ofMillis(1500));
            assertThat(bucket.availableTokens()).isEqualTo(1);

            // 1.5 periods, then 0.5 more: two whole periods have passed, so two refills.
            time.advance(Duration.ofMillis(500));
            assertThat(bucket.availableTokens()).isEqualTo(2);
        }

        @Test
        void a_full_bucket_restarts_its_clock_so_the_next_token_comes_one_full_period_after_spending() {
            TokenBucket bucket = bucket(5, 1);
            time.advance(PERIOD.multipliedBy(10)); // ten periods of waiting cannot store more than 5 tokens

            assertThat(bucket.tryConsume(5)).isTrue();

            time.advance(Duration.ofMillis(999));
            assertThat(bucket.availableTokens()).isZero();
            time.advance(Duration.ofMillis(1));
            assertThat(bucket.availableTokens()).isEqualTo(1);
        }

        @Test
        void a_bucket_that_is_refilled_to_full_restarts_its_clock_at_that_moment() {
            TokenBucket bucket = bucket(5, 1);
            bucket.tryConsume(1); // 4 left, refill clock still at START

            time.advance(Duration.ofMillis(1500));
            assertThat(bucket.availableTokens()).isEqualTo(5); // full at 1.5 s

            assertThat(bucket.tryConsume(5)).isTrue(); // empty now, clock restarts at 1.5 s
            time.advance(Duration.ofMillis(999));
            assertThat(bucket.availableTokens()).isZero();
            time.advance(Duration.ofMillis(1));
            assertThat(bucket.availableTokens()).isEqualTo(1);
        }

        @Test
        void a_tiny_period_and_a_long_gap_fill_the_bucket_without_an_exception() {
            TokenBucket bucket = new TokenBucket(5, 1, Duration.ofNanos(1), time);
            assertThat(bucket.tryConsume(5)).isTrue();

            // About 1.3e19 nanoseconds, more than a long can count (about 9.2e18). The bucket must still be full.
            time.advance(Duration.ofDays(400 * 365L));

            assertThat(bucket.availableTokens()).isEqualTo(5);
        }
    }

    @Nested
    class TimeGoingBackwards {

        @Test
        void neither_adds_nor_removes_tokens() {
            TokenBucket bucket = bucket(5, 1);
            bucket.tryConsume(2); // 3 left

            time.set(START.minus(Duration.ofHours(1)));
            assertThat(bucket.availableTokens()).isEqualTo(3);

            time.set(START.plus(PERIOD));
            assertThat(bucket.availableTokens()).isEqualTo(4);
        }
    }

    @Nested
    class Validation {

        @ParameterizedTest
        @CsvSource({"0,1,PT1S", "-3,1,PT1S", "5,0,PT1S", "5,-1,PT1S", "5,1,PT0S", "5,1,-PT1S"})
        void rejects_invalid_parameters(long capacity, long refillTokens, String refillPeriod) {
            assertThatThrownBy(() -> new TokenBucket(capacity, refillTokens, Duration.parse(refillPeriod), time))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void rejects_null_arguments_and_names_them() {
            assertThatThrownBy(() -> new TokenBucket(5, 1, null, time))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("refillPeriod");
            assertThatThrownBy(() -> new TokenBucket(5, 1, PERIOD, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("time");
        }

        @Test
        void accepts_the_smallest_valid_settings() {
            TokenBucket bucket = new TokenBucket(1, 1, Duration.ofNanos(1), time);

            assertThat(bucket.tryConsume()).isTrue();
            assertThat(bucket.tryConsume()).isFalse();
        }
    }
}
