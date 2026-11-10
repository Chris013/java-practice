package io.github.chris013.practice.task36;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * One request in the shop backend's access log.
 *
 * <p>Lines look like {@code 2026-03-02T14:03:11Z anna GET /api/orders/42 200 87}: time, user, method, path,
 * HTTP status and duration in milliseconds, separated by single spaces.
 */
public record LogEntry(Instant time, String user, String method, String path, int status, long durationMs) {

    public LogEntry {
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(path, "path");
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("path must start with '/', was " + path);
        }
        if (status < 100 || status > 599) {
            throw new IllegalArgumentException("status must be between 100 and 599, was " + status);
        }
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs must not be negative, was " + durationMs);
        }
    }

    /** Parses one log line. Any malformed line is rejected, and the message contains the line. */
    public static LogEntry parse(String line) {
        Objects.requireNonNull(line, "line");
        String[] fields = line.split(" ", -1);
        if (fields.length != 6) {
            throw new IllegalArgumentException("expected 6 fields in log line: " + line);
        }
        try {
            return new LogEntry(
                    Instant.parse(fields[0]),
                    fields[1],
                    fields[2],
                    fields[3],
                    Integer.parseInt(fields[4]),
                    Long.parseLong(fields[5]));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            throw new IllegalArgumentException("malformed log line: " + line, e);
        }
    }

    public boolean isServerError() {
        return status >= 500;
    }

    public boolean isClientError() {
        return status >= 400 && status < 500;
    }
}
