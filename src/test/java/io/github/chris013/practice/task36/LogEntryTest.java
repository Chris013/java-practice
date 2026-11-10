package io.github.chris013.practice.task36;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LogEntryTest {

    @Test
    void parses_a_well_formed_line() {
        LogEntry entry = LogEntry.parse("2026-03-02T14:03:11Z anna GET /api/orders/42 200 87");

        assertThat(entry.time()).isEqualTo(Instant.parse("2026-03-02T14:03:11Z"));
        assertThat(entry.user()).isEqualTo("anna");
        assertThat(entry.method()).isEqualTo("GET");
        assertThat(entry.path()).isEqualTo("/api/orders/42");
        assertThat(entry.status()).isEqualTo(200);
        assertThat(entry.durationMs()).isEqualTo(87L);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "too few fields | 2026-03-02T14:03:11Z anna GET /api/orders/42 200",
        "double space | 2026-03-02T14:03:11Z  anna GET /api/orders/42 200 87",
        "bad time | yesterday anna GET /api/orders/42 200 87",
        "bad status | 2026-03-02T14:03:11Z anna GET /api/orders/42 OK 87",
        "status 600 | 2026-03-02T14:03:11Z anna GET /api/orders/42 600 87",
        "negative duration | 2026-03-02T14:03:11Z anna GET /api/orders/42 200 -1",
        "path without slash | 2026-03-02T14:03:11Z anna GET api/orders/42 200 87"
    })
    void rejects_a_malformed_line_and_names_the_line(String caseName, String line) {
        assertThatThrownBy(() -> LogEntry.parse(line))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(line);
    }

    @Nested
    class Construction {

        @Test
        void accepts_the_edges_of_the_status_range_and_a_zero_duration() {
            Instant time = Instant.parse("2026-03-02T14:03:11Z");

            assertThatCode(() -> new LogEntry(time, "anna", "GET", "/", 100, 0)).doesNotThrowAnyException();
            assertThatCode(() -> new LogEntry(time, "anna", "GET", "/", 599, 0)).doesNotThrowAnyException();
        }

        @Test
        void rejects_null_fields() {
            assertThatThrownBy(() -> new LogEntry(null, "anna", "GET", "/", 200, 1))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new LogEntry(Instant.EPOCH, null, "GET", "/", 200, 1))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class Classification {

        @ParameterizedTest
        @CsvSource({"399, false", "400, false", "499, false", "500, true", "599, true"})
        void server_errors_are_500_to_599(int status, boolean expected) {
            assertThat(entryWithStatus(status).isServerError()).isEqualTo(expected);
        }

        @ParameterizedTest
        @CsvSource({"399, false", "400, true", "499, true", "500, false"})
        void client_errors_are_400_to_499(int status, boolean expected) {
            assertThat(entryWithStatus(status).isClientError()).isEqualTo(expected);
        }

        private static LogEntry entryWithStatus(int status) {
            return new LogEntry(Instant.parse("2026-03-02T14:03:11Z"), "anna", "GET", "/", status, 1);
        }
    }
}
