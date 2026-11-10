package io.github.chris013.practice.task36;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * One day's access log, sorted by time, and the questions the ops team asks of it.
 *
 * <p>The log is immutable. Every query returns a new value and changes nothing.
 */
public final class AccessLog {

    private final List<LogEntry> entries;

    private AccessLog(List<LogEntry> entries) {
        this.entries = entries;
    }

    /**
     * Copies the entries. Equal times are allowed, but the list must be sorted by time.
     *
     * @throws IllegalArgumentException if an entry is earlier than the one before it; the message names its index
     */
    public static AccessLog of(List<LogEntry> entries) {
        List<LogEntry> copy = List.copyOf(Objects.requireNonNull(entries, "entries"));
        // Sortedness is checked once, here. Every query after this relies on it, so the check cannot be skipped.
        OptionalInt firstOutOfOrder = IntStream.range(1, copy.size())
                .filter(i -> copy.get(i).time().isBefore(copy.get(i - 1).time()))
                .findFirst();
        if (firstOutOfOrder.isPresent()) {
            throw new IllegalArgumentException(
                    "entries must be sorted by time, but the entry at index %d is earlier than the one before it"
                            .formatted(firstOutOfOrder.getAsInt()));
        }
        return new AccessLog(copy);
    }

    /** Parses the lines. Blank lines and lines that start with {@code #} are skipped. */
    public static AccessLog parse(List<String> lines) {
        Objects.requireNonNull(lines, "lines");
        List<LogEntry> entries = lines.stream()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(LogEntry::parse)
                .toList();
        return of(entries);
    }

    /**
     * Entries with {@code fromInclusive <= time < toExclusive}, in log order.
     *
     * <p>{@code dropWhile} and {@code takeWhile} are only correct because the log is sorted. {@code dropWhile}
     * skips the early entries. {@code takeWhile} stops at the first entry at or after {@code toExclusive}, so the
     * rest of the day is never looked at. A {@code filter} would examine every entry.
     *
     * @throws IllegalArgumentException if {@code fromInclusive} is after {@code toExclusive}
     */
    public List<LogEntry> between(Instant fromInclusive, Instant toExclusive) {
        Objects.requireNonNull(fromInclusive, "fromInclusive");
        Objects.requireNonNull(toExclusive, "toExclusive");
        if (fromInclusive.isAfter(toExclusive)) {
            throw new IllegalArgumentException("from %s is after to %s".formatted(fromInclusive, toExclusive));
        }
        return entries.stream()
                .dropWhile(entry -> entry.time().isBefore(fromInclusive))
                .takeWhile(entry -> entry.time().isBefore(toExclusive))
                .toList();
    }

    /** The first entry with a 5xx status, if there is one. */
    public Optional<LogEntry> firstServerError() {
        return entries.stream()
                .filter(LogEntry::isServerError)
                .findFirst();
    }

    /** Distinct user names, in the order they first appear. {@code distinct} keeps the first occurrence. */
    public List<String> usersSeen() {
        return entries.stream()
                .map(LogEntry::user)
                .distinct()
                .toList();
    }

    /**
     * Every distinct non-empty segment of every path, sorted. {@code flatMap} turns each path into a stream of
     * segments and merges them into one stream. A plain {@code map} would give a stream of arrays.
     */
    public SortedSet<String> pathSegments() {
        SortedSet<String> segments = entries.stream()
                .map(LogEntry::path)
                .flatMap(path -> Arrays.stream(path.split("/")))
                .filter(segment -> !segment.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
        return Collections.unmodifiableSortedSet(segments);
    }

    /**
     * Splits the entries by error. {@code partitioningBy} always gives both keys, even when one side is empty.
     * {@code toUnmodifiableList} makes the two lists immutable.
     */
    public Map<Boolean, List<LogEntry>> partitionByError() {
        return entries.stream()
                .collect(Collectors.partitioningBy(
                        entry -> entry.isClientError() || entry.isServerError(),
                        Collectors.toUnmodifiableList()));
    }

    /**
     * Count, sum, min, max and average of the durations. {@code mapToLong} keeps the values primitive, so nothing
     * is boxed. For an empty log, count is 0 and min and max carry the usual {@code LongSummaryStatistics} defaults.
     */
    public LongSummaryStatistics durations() {
        return entries.stream()
                .mapToLong(LogEntry::durationMs)
                .summaryStatistics();
    }

    /** How many entries took strictly longer than {@code threshold}, compared in whole milliseconds. */
    public long countSlowerThan(Duration threshold) {
        Objects.requireNonNull(threshold, "threshold");
        long thresholdMs = threshold.toMillis();
        return entries.stream()
                .mapToLong(LogEntry::durationMs)
                .filter(durationMs -> durationMs > thresholdMs)
                .count();
    }

    /**
     * Counts entries per window {@code [start + i*window, start + (i+1)*window)} for every window that starts
     * before {@code end}. {@code Stream.iterate} with a predicate stops at {@code end}, so no empty trailing window
     * is produced. Each window reuses {@link #between}, which also keeps the early-stopping behaviour.
     *
     * @throws IllegalArgumentException if {@code window} is not positive or {@code start} is after {@code end}
     */
    public List<Long> requestsPerWindow(Instant start, Instant end, Duration window) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        Objects.requireNonNull(window, "window");
        if (!window.isPositive()) {
            throw new IllegalArgumentException("window must be positive, was " + window);
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("start %s is after end %s".formatted(start, end));
        }
        return Stream.iterate(start, windowStart -> windowStart.isBefore(end), windowStart -> windowStart.plus(window))
                .map(windowStart -> (long) between(windowStart, earlier(windowStart.plus(window), end)).size())
                .toList();
    }

    private static Instant earlier(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }
}
