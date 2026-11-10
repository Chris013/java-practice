package io.github.chris013.practice.task36;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AccessLogTest {

    private static final String LOG = """
            # shop backend access log, 2026-03-02
            2026-03-02T14:00:02Z anna GET /api/products 200 41

            2026-03-02T14:00:59Z ben POST /api/cart/items 201 120
            2026-03-02T14:03:11Z anna GET /api/orders/42 200 87
            2026-03-02T14:03:11Z cara GET /api/orders/7 404 15
            2026-03-02T14:07:45Z ben GET /api/orders/42 500 1530
            2026-03-02T14:10:00Z dan GET /api/products/9 200 640
            2026-03-02T14:12:30Z anna POST /api/orders 503 2210
            2026-03-02T14:14:59Z cara GET /api/products 200 38
            2026-03-02T14:15:00Z dan GET /api/orders/42 200 95
            2026-03-02T14:16:20Z ben GET /api/orders/99 401 12
            2026-03-02T14:20:00Z eve GET /health 200 3
            2026-03-02T14:20:00Z anna GET /api/orders/42 200 402
            """;

    private final AccessLog day = AccessLog.parse(LOG.lines().toList());

    private static Instant at(String time) {
        return Instant.parse("2026-03-02T" + time + "Z");
    }

    private static LogEntry entry(String time, int status) {
        return new LogEntry(at(time), "anna", "GET", "/api/orders/42", status, 10);
    }

    @Nested
    class Parsing {

        @Test
        void skips_blank_lines_and_comments() {
            assertThat(day.durations().getCount()).isEqualTo(12);
        }

        @Test
        void a_log_with_only_comments_is_empty() {
            assertThat(AccessLog.parse(List.of("# nothing today", ""))
                    .durations().getCount()).isZero();
        }

        @Test
        void a_malformed_line_fails_and_the_message_names_it() {
            List<String> lines = List.of("2026-03-02T14:00:00Z anna GET /x 200 10", "2026-03-02T14:00:01Z broken");

            assertThatThrownBy(() -> AccessLog.parse(lines))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("broken");
        }
    }

    @Nested
    class Of {

        @Test
        void accepts_equal_times() {
            AccessLog log = AccessLog.of(List.of(entry("14:00:00", 200), entry("14:00:00", 200)));

            assertThat(log.durations().getCount()).isEqualTo(2);
        }

        @Test
        void rejects_unsorted_entries_and_names_the_first_bad_index() {
            List<LogEntry> unsorted = List.of(
                    entry("14:00:00", 200),
                    entry("14:05:00", 200),
                    entry("14:01:00", 200),
                    entry("14:02:00", 200));

            assertThatThrownBy(() -> AccessLog.of(unsorted))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("index 2");
        }

        @Test
        void copies_its_input() {
            List<LogEntry> input = new ArrayList<>(List.of(entry("14:00:00", 200)));
            AccessLog log = AccessLog.of(input);

            input.clear();

            assertThat(log.durations().getCount()).isEqualTo(1);
        }

        @Test
        void rejects_null() {
            assertThatThrownBy(() -> AccessLog.of(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Between {

        @Test
        void includes_the_entries_at_from_and_excludes_the_entries_at_to() {
            List<LogEntry> window = day.between(at("14:03:11"), at("14:10:00"));

            assertThat(window).extracting(LogEntry::user).containsExactly("anna", "cara", "ben");
        }

        @Test
        void includes_the_first_entry_when_from_is_its_time() {
            assertThat(day.between(at("14:00:02"), at("14:00:03")))
                    .singleElement()
                    .satisfies(entry -> assertThat(entry.durationMs()).isEqualTo(41L));
        }

        @Test
        void an_empty_range_gives_nothing() {
            assertThat(day.between(at("14:15:00"), at("14:15:00"))).isEmpty();
            assertThat(day.between(at("13:00:00"), at("14:00:02"))).isEmpty();
        }

        @Test
        void a_range_ending_at_the_last_time_leaves_out_that_time() {
            assertThat(day.between(at("14:16:20"), at("14:20:00")))
                    .extracting(LogEntry::user)
                    .containsExactly("ben");
        }

        @Test
        void a_range_covering_the_tail_of_the_day_returns_the_tail() {
            assertThat(day.between(at("14:20:00"), at("23:59:59")))
                    .extracting(LogEntry::user)
                    .containsExactly("eve", "anna");
        }

        @Test
        void rejects_a_range_that_ends_before_it_starts() {
            assertThatThrownBy(() -> day.between(at("14:10:00"), at("14:00:00")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void returns_an_unmodifiable_list() {
            List<LogEntry> window = day.between(at("14:00:00"), at("14:01:00"));

            assertThatThrownBy(() -> window.add(entry("14:00:30", 200)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class FirstServerError {

        @Test
        void returns_the_first_5xx_entry_in_time_order() {
            assertThat(day.firstServerError()).hasValueSatisfying(entry -> {
                assertThat(entry.time()).isEqualTo(at("14:07:45"));
                assertThat(entry.status()).isEqualTo(500);
            });
        }

        @Test
        void is_empty_when_only_4xx_and_2xx_entries_exist() {
            AccessLog log = AccessLog.of(List.of(entry("14:00:00", 404), entry("14:00:01", 200)));

            assertThat(log.firstServerError()).isEmpty();
        }
    }

    @Nested
    class UsersAndPaths {

        @Test
        void lists_each_user_once_in_order_of_first_appearance() {
            assertThat(day.usersSeen()).containsExactly("anna", "ben", "cara", "dan", "eve");
        }

        @Test
        void users_list_is_unmodifiable() {
            assertThatThrownBy(() -> day.usersSeen().add("zoe")).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void collects_every_distinct_path_segment_in_sorted_order() {
            assertThat(day.pathSegments()).containsExactly(
                    "42", "7", "9", "99", "api", "cart", "health", "items", "orders", "products");
        }

        @Test
        void path_segments_are_unmodifiable() {
            assertThatThrownBy(() -> day.pathSegments().add("x")).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void ignores_empty_segments_so_the_root_path_contributes_nothing() {
            AccessLog log = AccessLog.of(List.of(new LogEntry(at("14:00:00"), "anna", "GET", "/", 200, 1)));

            assertThat(log.pathSegments()).isEmpty();
        }
    }

    @Nested
    class Statistics {

        @Test
        void partitions_by_error_keeping_log_order() {
            assertThat(day.partitionByError().get(true))
                    .extracting(LogEntry::status)
                    .containsExactly(404, 500, 503, 401);
            assertThat(day.partitionByError().get(false)).hasSize(8);
        }

        @Test
        void both_partition_keys_exist_even_without_errors() {
            AccessLog log = AccessLog.of(List.of(entry("14:00:00", 200)));

            assertThat(log.partitionByError()).containsOnlyKeys(true, false);
            assertThat(log.partitionByError().get(true)).isEmpty();
        }

        @Test
        void partition_lists_are_unmodifiable() {
            assertThatThrownBy(() -> day.partitionByError().get(true).add(entry("14:00:00", 500)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void summarises_the_durations() {
            var stats = day.durations();

            assertThat(stats.getCount()).isEqualTo(12);
            assertThat(stats.getSum()).isEqualTo(5193L);
            assertThat(stats.getMin()).isEqualTo(3L);
            assertThat(stats.getMax()).isEqualTo(2210L);
        }

        @ParameterizedTest(name = "slower than {0} ms: {1} entries")
        @CsvSource({"0, 12", "500, 3", "640, 2", "2210, 0"})
        void counts_entries_strictly_slower_than_the_threshold(long thresholdMs, long expected) {
            assertThat(day.countSlowerThan(Duration.ofMillis(thresholdMs))).isEqualTo(expected);
        }

        @Test
        void an_empty_log_has_no_requests() {
            AccessLog empty = AccessLog.of(List.of());

            assertThat(empty.durations().getCount()).isZero();
            assertThat(empty.countSlowerThan(Duration.ZERO)).isZero();
        }
    }

    @Nested
    class RequestsPerWindow {

        @Test
        void counts_requests_in_each_five_minute_window() {
            assertThat(day.requestsPerWindow(at("14:00:00"), at("14:20:00"), Duration.ofMinutes(5)))
                    .containsExactly(4L, 1L, 3L, 2L);
        }

        @Test
        void the_last_window_counts_only_entries_before_the_end() {
            assertThat(day.requestsPerWindow(at("14:00:00"), at("14:12:00"), Duration.ofMinutes(5)))
                    .containsExactly(4L, 1L, 1L);
        }

        @Test
        void an_end_on_a_window_boundary_adds_no_extra_window() {
            assertThat(day.requestsPerWindow(at("14:00:00"), at("14:05:00"), Duration.ofMinutes(5)))
                    .containsExactly(4L);
        }

        @Test
        void a_window_starting_mid_hour_includes_entries_at_its_start_time() {
            assertThat(day.requestsPerWindow(at("14:03:11"), at("14:08:00"), Duration.ofMinutes(5)))
                    .containsExactly(3L);
        }

        @Test
        void quiet_windows_count_as_zero() {
            AccessLog log = AccessLog.of(List.of(entry("14:00:00", 200), entry("14:12:00", 200)));

            assertThat(log.requestsPerWindow(at("14:00:00"), at("14:15:00"), Duration.ofMinutes(5)))
                    .containsExactly(1L, 0L, 1L);
        }

        @Test
        void equal_start_and_end_gives_no_windows() {
            assertThat(day.requestsPerWindow(at("14:00:00"), at("14:00:00"), Duration.ofMinutes(5))).isEmpty();
        }

        @Test
        void an_empty_log_gives_zero_per_window() {
            assertThat(AccessLog.of(List.of()).requestsPerWindow(at("14:00:00"), at("14:10:00"), Duration.ofMinutes(5)))
                    .containsExactly(0L, 0L);
        }

        @Test
        void rejects_a_start_after_the_end() {
            assertThatThrownBy(() -> day.requestsPerWindow(at("14:10:00"), at("14:00:00"), Duration.ofMinutes(5)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest
        @CsvSource({"PT0S", "PT-1S"})
        void rejects_a_window_that_is_not_positive(String window) {
            assertThatThrownBy(() -> day.requestsPerWindow(at("14:00:00"), at("14:10:00"), Duration.parse(window)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
