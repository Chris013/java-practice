package io.github.chris013.practice.task36;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;

/**
 * One day's access log, sorted by time, and the questions the ops team asks of it.
 *
 * <p>Task 36: read {@code tasks/36-access-log-streams.md} first.
 *
 * <p>The log is immutable. Every query returns a new value and changes nothing.
 */
public final class AccessLog {

    private final List<LogEntry> entries;

    private AccessLog(List<LogEntry> entries) {
        throw new UnsupportedOperationException("TODO: Task 36");
    }

    /**
     * Copies the entries. Equal times are allowed, but the list must be sorted by time.
     *
     * @throws IllegalArgumentException if an entry is earlier than the one before it; the message names its index
     */
    public static AccessLog of(List<LogEntry> entries) {
        throw new UnsupportedOperationException("TODO: Task 36");
    }

    /** Parses the lines. Blank lines and lines that start with {@code #} are skipped. */
    public static AccessLog parse(List<String> lines) {
        throw new UnsupportedOperationException("TODO: Task 36");
    }

    /**
     * Entries with {@code fromInclusive <= time < toExclusive}, in log order.
     *
     * @throws IllegalArgumentException if {@code fromInclusive} is after {@code toExclusive}
     */
    public List<LogEntry> between(Instant fromInclusive, Instant toExclusive) {
        throw new UnsupportedOperationException("TODO: Task 36");
    }

    /** The first entry with a 5xx status, if there is one. */
    public Optional<LogEntry> firstServerError() {
        throw new UnsupportedOperationException("TODO: Task 36");
    }

    /** Distinct user names, in the order they first appear. */
    public List<String> usersSeen() {
        throw new UnsupportedOperationException("TODO: Task 36");
    }

    /** Every distinct non-empty segment of every path, sorted. {@code /api/orders/42} gives api, orders, 42. */
    public SortedSet<String> pathSegments() {
        throw new UnsupportedOperationException("TODO: Task 36");
    }

    /** Entries split by error: key {@code true} for status 400 and up, key {@code false} otherwise. Both keys exist. */
    public Map<Boolean, List<LogEntry>> partitionByError() {
        throw new UnsupportedOperationException("TODO: Task 36");
    }

    /** Count, sum, min, max and average of the durations. For an empty log, count is 0. */
    public LongSummaryStatistics durations() {
        throw new UnsupportedOperationException("TODO: Task 36");
    }

    /** How many entries took strictly longer than {@code threshold}, compared in whole milliseconds. */
    public long countSlowerThan(Duration threshold) {
        throw new UnsupportedOperationException("TODO: Task 36");
    }

    /**
     * Counts entries per window {@code [start + i*window, start + (i+1)*window)} for every window that starts
     * before {@code end}. Only entries before {@code end} are counted, even in a last window that runs past it.
     *
     * @throws IllegalArgumentException if {@code window} is not positive or {@code start} is after {@code end}
     */
    public List<Long> requestsPerWindow(Instant start, Instant end, Duration window) {
        throw new UnsupportedOperationException("TODO: Task 36");
    }
}
